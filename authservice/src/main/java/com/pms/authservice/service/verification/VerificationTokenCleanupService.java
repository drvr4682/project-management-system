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

    /**
     * Daily cleanup job to prune verification tokens older than 30 days.
     * Runs once daily at midnight.
     */
    @Scheduled(cron = "${app.verification-cleanup.cron:0 0 0 * * ?}")
    @Transactional
    public void cleanupTokens() {
        log.info("[Verification Cleanup] Starting daily scheduled token cleanup...");
        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        try {
            int deletedCount = verificationTokenRepository.deleteExpiredOrUsedTokensOlderThan(limit);
            log.info("[Verification Cleanup] Cleanup completed successfully. Removed {} expired/used tokens older than 30 days.", deletedCount);
        } catch (Exception e) {
            log.error("[Verification Cleanup] Daily token cleanup failed: {}", e.getMessage(), e);
        }
    }
}
