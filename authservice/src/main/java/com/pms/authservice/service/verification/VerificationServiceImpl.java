package com.pms.authservice.service.verification;

import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import com.pms.authservice.exception.ExpiredVerificationTokenException;
import com.pms.authservice.exception.InvalidVerificationTokenException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.email-verification.token-expiry-minutes:1440}")
    private long tokenExpiryMinutes;

    @Override
    @Transactional
    public VerificationToken createVerificationToken(User user) {
        log.info("[Verification] Generating a new verification token for user ID: {}", user.getId());

        // Delete any existing token for this user first to avoid unique constraint violations
        verificationTokenRepository.deleteByUserId(user.getId());

        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(tokenExpiryMinutes);

        VerificationToken vToken = VerificationToken.builder()
                .token(tokenValue)
                .user(user)
                .expiryDate(expiryDate)
                .used(false)
                .build();

        VerificationToken savedToken = verificationTokenRepository.save(vToken);
        log.info("[Verification] Verification token successfully generated and saved for user: {}", user.getEmail());
        return savedToken;
    }

    @Override
    @Transactional
    public VerificationToken validateVerificationToken(String token) {
        log.info("[Verification] Validating token: {}", token);

        if (token == null || token.isBlank()) {
            log.warn("[Verification] Empty token provided");
            throw new InvalidVerificationTokenException("Verification token is missing");
        }

        VerificationToken vToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("[Verification] Invalid verification token: {}", token);
                    return new InvalidVerificationTokenException("Invalid verification token");
                });

        if (vToken.isUsed()) {
            log.warn("[Verification] Token already used: {}", token);
            throw new InvalidVerificationTokenException("This verification link has already been used");
        }

        if (vToken.isExpired()) {
            log.warn("[Verification] Token expired: {}", token);
            throw new ExpiredVerificationTokenException("Verification link has expired");
        }

        User user = vToken.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        vToken.setUsed(true);
        VerificationToken savedToken = verificationTokenRepository.save(vToken);

        log.info("[Verification] User {} successfully verified and account enabled", user.getEmail());
        return savedToken;
    }
}
