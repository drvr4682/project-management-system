package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.dto.ChangePasswordRequest;
import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import com.pms.authservice.exception.EmailVerificationException;
import com.pms.authservice.exception.InvalidCredentialsException;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.exception.UserNotFoundException;
import com.pms.authservice.exception.TooManyRequestsException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.service.email.EmailService;
import com.pms.authservice.service.verification.VerificationService;
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
    private final VerificationService         verificationService;
    private final PasswordEncoder             passwordEncoder;
    private final JwtUtil                     jwtUtil;
    private final AuthenticationManager       authenticationManager;
    private final RefreshTokenService         refreshTokenService;
    private final StringRedisTemplate         redisTemplate;
    private final EmailService                emailService;

    @Value("${app.email-verification.token-expiry-minutes:1440}")
    private long verificationTokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    private final java.util.concurrent.ConcurrentHashMap<String, LocalDateTime> resendCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    // =========================================================================
    // REGISTER
    // =========================================================================

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String userName = request.getUserName().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "User already exists with email: " + email);
        }

        if (userRepository.existsByUserName(userName)) {
            throw new UserAlreadyExistsException(
                    "User already exists with username: " + userName);
        }

        // 1. Persist user — no token fields on the entity
        User user = User.builder()
                .id(java.util.UUID.randomUUID())
                .userName(userName)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(com.pms.authservice.entity.Role.USER) // Default role
                .emailVerified(false)
                .enabled(false)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        // 2. Create a VerificationToken record
        VerificationToken vToken = verificationService.createVerificationToken(savedUser);

        // 3. Build verification URL pointing to the frontend
        String verificationLink = frontendBaseUrl + "/verify-email?token=" + vToken.getToken();

        // 4. Send verification email using username
        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getUserName(),
                verificationLink
        );

        log.info("[Auth] Registered new user (unverified): {} | role: {}",
                savedUser.getEmail(), savedUser.getRole());

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .userName(savedUser.getUserName())
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

        String identifier = request.getEmailOrUsername() == null
                ? ""
                : request.getEmailOrUsername().trim().toLowerCase();

        // 0. Enforce rate limiting per identifier (Primary: Redis, Fallback: none/passthrough)
        if (!identifier.isEmpty()) {
            String rateLimitKey = "rate:limit:login:" + identifier;
            boolean isBlocked = false;
            try {
                String val = redisTemplate.opsForValue().get(rateLimitKey);
                if (val != null && Integer.parseInt(val) >= 5) {
                    isBlocked = true;
                }
            } catch (Exception e) {
                log.warn("[Auth] Redis is down or unavailable for rate limiting. Bypassing check: {}", e.getMessage());
            }

            if (isBlocked) {
                log.warn("[Auth] Login rate limit exceeded for identifier: {}", identifier);
                throw new TooManyRequestsException("Too many login attempts. Please try again in 1 minute.");
            }

            try {
                Long count = redisTemplate.opsForValue().increment(rateLimitKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(rateLimitKey, 60, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("[Auth] Redis is down. Unable to increment rate limit counter for: {}", identifier);
            }
        }

        // 1. Authenticate credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email/username or password");
        }

        // 2. Load user
        User user = userRepository.findByEmailOrUserName(identifier, identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email/username or password"));

        // 3. Block unverified accounts
        if (!user.isEmailVerified()) {
            log.warn("[Auth] Login attempt for unverified account: {}", identifier);
            throw new EmailVerificationException(
                    "Email address is not verified. "
                    + "Please check your inbox and click the verification link.");
        }

        // 4. Issue tokens
        String accessToken  = jwtUtil.generateToken(user.getId().toString(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("[Auth] Login successful: {}", user.getEmail());

        // 5. Clear rate limit counter on successful login
        if (!identifier.isEmpty()) {
            String rateLimitKey = "rate:limit:login:" + identifier;
            try {
                redisTemplate.delete(rateLimitKey);
            } catch (Exception e) {
                log.warn("[Auth] Redis is down. Unable to clear rate limit counter for: {}", identifier);
            }
        }

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRole().name())
                .id(user.getId())
                .userName(user.getUserName())
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
    public void logout(String userIdStr, String accessToken) {

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
                                userIdStr, jti, remainingMs);
                    }
                }
            } catch (Exception e) {
                // Redis failure must not prevent logout from completing
                log.warn("[Auth] Failed to blocklist access token for user {}: {}",
                        userIdStr, e.getMessage());
            }
        }

        // Revoke all refresh tokens in DB
        if (userIdStr != null) {
            try {
                java.util.UUID userId = java.util.UUID.fromString(userIdStr);
                refreshTokenService.revokeAll(userId);
            } catch (Exception ex) {
                log.error("Failed to revoke refresh tokens for user ID: {}", userIdStr, ex);
            }
        }

        log.info("[Auth] Logout complete for user: {}", userIdStr);
    }

    // =========================================================================
    // EMAIL VERIFICATION — verify link click
    // =========================================================================

    @Override
    @Transactional
    public void verifyEmail(String token) {
        log.info("[Auth] Verification request received for token: {}", token);
        verificationService.validateVerificationToken(token);
    }

    // =========================================================================
    // EMAIL VERIFICATION — resend link
    // =========================================================================

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // 1. Enforce 60-second cooldown protection (Primary: Redis, Fallback: In-memory)
        String key = "cooldown:resend:" + email;
        boolean hasCooldown = false;
        try {
            hasCooldown = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("[Auth] Redis is down or unavailable. Falling back to in-memory check: {}", e.getMessage());
        }

        // Secondary check: if Redis is not active or mock returns false, verify the local fallback map
        if (!hasCooldown) {
            LocalDateTime lastSent = resendCooldowns.get(email);
            if (lastSent != null && lastSent.plusSeconds(60).isAfter(LocalDateTime.now())) {
                hasCooldown = true;
            }
        }

        if (hasCooldown) {
            log.warn("[Auth] Resend verification cooldown active for email: {}", email);
            throw new EmailVerificationException("Please wait 60 seconds before requesting another verification email.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(
                        "No account found with email: " + email));

        if (user.isEmailVerified()) {
            log.warn("[Auth] Resend verification requested for already-verified email: {}", email);
            throw new EmailVerificationException("Email is already verified");
        }

        // 2. Issue a fresh token using VerificationService
        VerificationToken newToken = verificationService.createVerificationToken(user);

        // 3. Build verification link pointing to the frontend
        String verificationLink = frontendBaseUrl + "/verify-email?token=" + newToken.getToken();

        // 4. Send email
        String fullName = user.getUserName();
        emailService.sendVerificationEmail(user.getEmail(), fullName, verificationLink);

        // 5. Update cooldown timestamp (Primary: Redis, Fallback: In-memory)
        try {
            redisTemplate.opsForValue().set(key, "1", 60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[Auth] Redis is down. Persisting cooldown in-memory for email: {}", email);
            resendCooldowns.put(email, LocalDateTime.now());
        }

        log.info("[Auth] Verification email successfully resent to: {}", email);
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    @Override
    public boolean userExists(java.util.UUID id) {
        return userRepository.existsById(id);
    }

    @Override
    @Transactional
    public void changePassword(String userIdStr, ChangePasswordRequest request) {
        log.info("[Auth] Password change request for user: {}", userIdStr);
        try {
            java.util.UUID userId = java.util.UUID.fromString(userIdStr);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new InvalidCredentialsException("User session is invalid"));

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("[Auth] Current password mismatch for user: {}", userIdStr);
                throw new InvalidCredentialsException("Current password does not match");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            log.info("[Auth] Password updated successfully for user: {}", userIdStr);
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Auth] Database or runtime error during password change for user: {}", userIdStr, e);
            throw new InvalidCredentialsException("Unable to update password. Please try again.");
        }
    }
}