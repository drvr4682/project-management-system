package com.pms.authservice.repository;

import com.pms.authservice.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE PasswordResetToken p
        SET p.used = true
        WHERE p.user.id = :userId
        AND p.used = false
    """)
    int invalidateUnusedTokensByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
        DELETE FROM PasswordResetToken p
        WHERE (p.used = false AND p.expiryDate < :limit)
           OR (p.used = true AND p.createdAt < :limit)
    """)
    int deleteExpiredOrUsedTokensOlderThan(@Param("limit") LocalDateTime limit);
}