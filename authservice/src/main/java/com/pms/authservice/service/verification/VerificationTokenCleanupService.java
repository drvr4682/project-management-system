package com.pms.authservice.service.verification;

import com.pms.authservice.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenCleanupService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final com.pms.authservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Daily cleanup job to prune verification and password reset tokens older than 30 days.
     * Runs once daily at midnight.
     */
    @Scheduled(cron = "${app.verification-cleanup.cron:0 0 0 * * ?}")
    @Transactional
    public void cleanupTokens() {
        log.info("[Verification Cleanup] Starting daily scheduled token cleanup...");
        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        try {
            int deletedVerificationCount = verificationTokenRepository.deleteExpiredOrUsedTokensOlderThan(limit);
            int deletedResetCount = passwordResetTokenRepository.deleteExpiredOrUsedTokensOlderThan(limit);
            log.info("[Verification Cleanup] Cleanup completed successfully. Removed {} expired/used verification tokens and {} expired/used password reset tokens older than 30 days.", deletedVerificationCount, deletedResetCount);
        } catch (Exception e) {
            log.error("[Verification Cleanup] Daily token cleanup failed: {}", e.getMessage(), e);
        }
    }
}
