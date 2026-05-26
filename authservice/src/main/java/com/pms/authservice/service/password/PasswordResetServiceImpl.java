package com.pms.authservice.service.password;

import com.pms.authservice.dto.ForgotPasswordRequest;
import com.pms.authservice.dto.ResetPasswordRequest;
import com.pms.authservice.entity.PasswordResetToken;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.ExpiredPasswordResetTokenException;
import com.pms.authservice.exception.InvalidPasswordResetTokenException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.repository.PasswordResetTokenRepository;
import com.pms.authservice.service.email.EmailService;
import com.pms.authservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    private final ConcurrentHashMap<String, LocalDateTime> forgotPasswordCooldowns = new ConcurrentHashMap<>();

    @Value("${app.password-reset.token-expiry-minutes:30}")
    private long tokenExpiryMinutes;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void handleForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // 1. Enforce 60-second cooldown protection
        LocalDateTime lastSent = forgotPasswordCooldowns.get(email);
        if (lastSent != null && lastSent.plusSeconds(60).isAfter(now)) {
            // Anti-enumeration: do NOT send email, do NOT throw an exception, return generic success
            log.warn("[PasswordReset] Cooldown active for forgot password request: {}", email);
            return;
        }

        // 2. Load user
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Anti-enumeration: do NOT throw exception, return generic success
            log.info("[PasswordReset] Forgot password requested for non-existent email: {}", email);
            // Save request timestamp to protect against mapping enumeration timing attacks
            forgotPasswordCooldowns.put(email, now);
            return;
        }

        // 3. Invalidate older active reset tokens
        passwordResetTokenRepository.invalidateUnusedTokensByUserId(user.getId());

        // 4. Generate new UUID token
        String tokenValue = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                .used(false)
                .build();

        passwordResetTokenRepository.saveAndFlush(resetToken);

        // 5. Send plain text email pointing to the frontend
        String resetLink = frontendBaseUrl + "/reset-password?token=" + tokenValue;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);

        // 6. Record cooldown
        forgotPasswordCooldowns.put(email, now);
        log.info("[PasswordReset] Password reset email sent to: {}", email);
    }

    @Override
    @Transactional
    public void handleResetPassword(ResetPasswordRequest request) {
        String token = request.getToken();

        if (token == null || token.isBlank()) {
            throw new InvalidPasswordResetTokenException("Reset token is missing");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidPasswordResetTokenException("This password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new ExpiredPasswordResetTokenException("Password reset link has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.saveAndFlush(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.saveAndFlush(resetToken);

        // Revoke active sessions / refresh tokens
        try {
            refreshTokenService.revokeAll(user.getEmail());
            log.info("[PasswordReset] Revoked all refresh tokens after password reset for user: {}", user.getEmail());
        } catch (Exception e) {
            log.warn("[PasswordReset] Failed to revoke refresh tokens for user {} after password reset: {}", user.getEmail(), e.getMessage());
        }

        log.info("[PasswordReset] Password reset completed successfully for user: {}", user.getEmail());
    }

    public void clearCooldowns() {
        forgotPasswordCooldowns.clear();
        log.info("[PasswordReset] Cooldowns map cleared successfully.");
    }
}
