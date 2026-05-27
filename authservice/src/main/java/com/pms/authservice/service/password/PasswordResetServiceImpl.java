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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;
    private final java.util.concurrent.ConcurrentHashMap<String, LocalDateTime> forgotPasswordCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

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

        // 1. Enforce 60-second cooldown protection (Primary: Redis, Fallback: In-memory)
        String key = "cooldown:forgot-password:" + email;
        boolean hasCooldown = false;
        try {
            hasCooldown = Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("[PasswordReset] Redis is down or unavailable. Falling back to in-memory check: {}", e.getMessage());
        }

        // Secondary check: if Redis is not active or mock returns false, verify local fallback map
        if (!hasCooldown) {
            LocalDateTime lastSent = forgotPasswordCooldowns.get(email);
            if (lastSent != null && lastSent.plusSeconds(60).isAfter(LocalDateTime.now())) {
                hasCooldown = true;
            }
        }

        if (hasCooldown) {
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
            try {
                redisTemplate.opsForValue().set(key, "1", 60, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("[PasswordReset] Redis is down. Persisting cooldown in-memory for email: {}", email);
                forgotPasswordCooldowns.put(email, LocalDateTime.now());
            }
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
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUserName(), resetLink);

        // 6. Record cooldown (Primary: Redis, Fallback: In-memory)
        try {
            redisTemplate.opsForValue().set(key, "1", 60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[PasswordReset] Redis is down. Persisting cooldown in-memory for email: {}", email);
            forgotPasswordCooldowns.put(email, LocalDateTime.now());
        }
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
        try {
            java.util.Set<String> keys = redisTemplate.keys("cooldown:forgot-password:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("[PasswordReset] Redis is down. Failed to clear cooldown keys: {}", e.getMessage());
        }
        log.info("[PasswordReset] Cooldowns cleared successfully.");
    }
}
