package com.pms.authservice.repository;

import com.pms.authservice.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    List<VerificationToken> findByUserId(java.util.UUID userId);

    @Modifying
    void deleteByUserId(java.util.UUID userId);

    @Modifying
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationToken v SET v.used = true WHERE v.user.id = :userId AND v.used = false")
    int invalidateUnusedTokensByUserId(@Param("userId") java.util.UUID userId);

    @Modifying
    @Query("""
            DELETE FROM VerificationToken v 
            WHERE (v.used = false AND v.expiryDate < :limit) 
               OR (v.used = true AND v.createdAt < :limit)
    """)
    int deleteExpiredOrUsedTokensOlderThan(@Param("limit") LocalDateTime limit);

    @Query("""
            SELECT COUNT(v) FROM VerificationToken v
            WHERE v.user.id = :userId
            AND v.used = false
            AND v.expiryDate > :now
    """)
    long countActiveByUserId(@Param("userId") java.util.UUID userId, @Param("now") LocalDateTime now);
}