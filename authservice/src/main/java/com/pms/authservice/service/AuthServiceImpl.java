package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import com.pms.authservice.exception.EmailVerificationException;
import com.pms.authservice.exception.InvalidCredentialsException;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.exception.UserNotFoundException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.repository.VerificationTokenRepository;
import com.pms.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository              userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder             passwordEncoder;
    private final JwtUtil                     jwtUtil;
    private final AuthenticationManager       authenticationManager;
    private final RefreshTokenService         refreshTokenService;
    private final StringRedisTemplate         redisTemplate;
    private final EmailService                emailService;

    @Value("${app.email-verification.token-expiry-minutes:1440}")
    private long verificationTokenExpiryMinutes;

    // =========================================================================
    // REGISTER
    // =========================================================================

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + email);
        }

        // 1. Persist user — no token fields on the entity
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .emailVerified(false)
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);

        // 2. Create a VerificationToken record (separate lifecycle from user)
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now()
                .plusMinutes(verificationTokenExpiryMinutes);

        VerificationToken vToken = VerificationToken.builder()
                .token(tokenValue)
                .user(savedUser)
                .expiryDate(expiry)
                .used(false)
                .build();

        verificationTokenRepository.save(vToken);

        // 3. Send verification email (async — does not block registration response)
        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getName(),
                tokenValue
        );

        log.info("[Auth] Registered new user (unverified): {} | role: {}",
                savedUser.getEmail(), savedUser.getRole());

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase();

        // 1. Authenticate credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 2. Load user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 3. Block unverified accounts
        if (!user.isEmailVerified()) {
            log.warn("[Auth] Login attempt for unverified account: {}", email);
            throw new EmailVerificationException(
                    "Email address is not verified. "
                    + "Please check your inbox and click the verification link.");
        }

        // 4. Issue tokens
        String accessToken  = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        log.info("[Auth] Login successful: {}", user.getEmail());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    // =========================================================================
    // REFRESH
    // =========================================================================

    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        return refreshTokenService.rotate(request);
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    @Override
    @Transactional
    public void logout(String userEmail, String accessToken) {

        // Blocklist the access token in Redis so the gateway rejects it immediately
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                if (jwtUtil.validateToken(accessToken)) {
                    String jti         = jwtUtil.extractJti(accessToken);
                    long   remainingMs = jwtUtil.getExpirationMillis(accessToken);

                    if (jti != null && remainingMs > 0) {
                        redisTemplate.opsForValue()
                                .set("blocklist:jti:" + jti, "revoked",
                                        remainingMs, TimeUnit.MILLISECONDS);
                        log.info("[Auth] Access token blocklisted | user: {} | jti: {} | ttl: {}ms",
                                userEmail, jti, remainingMs);
                    }
                }
            } catch (Exception e) {
                // Redis failure must not prevent logout from completing
                log.warn("[Auth] Failed to blocklist access token for user {}: {}",
                        userEmail, e.getMessage());
            }
        }

        // Revoke all refresh tokens in DB
        refreshTokenService.revokeAll(userEmail);

        log.info("[Auth] Logout complete for user: {}", userEmail);
    }

    // =========================================================================
    // EMAIL VERIFICATION — verify link click
    // =========================================================================

    @Override
    @Transactional
    public void verifyEmail(String token) {

        if (token == null || token.isBlank()) {
            throw new EmailVerificationException("Verification token is missing");
        }

        // Look up the token in its own table — not on the User entity
        VerificationToken vToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new EmailVerificationException("Invalid or expired verification token"));

        User user = vToken.getUser();

        // Idempotent — if already verified just succeed silently
        if (user.isEmailVerified()) {
            log.info("[Auth] Email already verified for user: {}", user.getEmail());
            return;
        }

        if (vToken.isUsed()) {
            throw new EmailVerificationException(
                    "This verification link has already been used.");
        }

        if (vToken.isExpired()) {
            throw new EmailVerificationException(
                    "Verification link has expired. Please request a new one.");
        }

        // Mark user as verified and enabled
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        // Mark token as used (single-use, cannot be replayed)
        vToken.setUsed(true);
        verificationTokenRepository.save(vToken);

        log.info("[Auth] Email verified successfully for user: {}", user.getEmail());
    }

    // =========================================================================
    // EMAIL VERIFICATION — resend link
    // =========================================================================

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        if (user.isEmailVerified()) {
            // Already verified — silent success (don't leak state)
            log.info("[Auth] Resend requested for already-verified account: {}", email);
            return;
        }

        // Delete any existing (old / expired) token for this user
        verificationTokenRepository.deleteByUserId(user.getId());

        // Issue a fresh token
        String newTokenValue = UUID.randomUUID().toString();
        LocalDateTime newExpiry = LocalDateTime.now()
                .plusMinutes(verificationTokenExpiryMinutes);

        VerificationToken newToken = VerificationToken.builder()
                .token(newTokenValue)
                .user(user)
                .expiryDate(newExpiry)
                .used(false)
                .build();

        verificationTokenRepository.save(newToken);

        emailService.sendResendVerificationEmail(user.getEmail(), user.getName(), newTokenValue);

        log.info("[Auth] Verification email resent to: {}", email);
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    @Override
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}