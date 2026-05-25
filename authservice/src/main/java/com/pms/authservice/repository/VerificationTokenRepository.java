package com.pms.authservice.repository;

import com.pms.authservice.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserId(Long userId);

    @Modifying
    void deleteByUserId(Long userId);

    @Modifying
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    @Query("""
            SELECT COUNT(v) FROM VerificationToken v
            WHERE v.user.id = :userId
            AND v.used = false
            AND v.expiryDate > :now
    """)
    long countActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}