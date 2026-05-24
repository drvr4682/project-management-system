package com.pms.authservice.repository;

import com.pms.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // clearAutomatically = true  → evicts ALL entities from the Hibernate L1
    //   session cache after the bulk UPDATE runs. Without it, any subsequent
    //   read in the same Hibernate session returns the stale pre-update object.
    //
    // flushAutomatically = true  → flushes any pending dirty-checked changes
    //   for managed entities to the DB BEFORE the bulk UPDATE executes.
    //   Without it, un-flushed in-memory changes for entities loaded earlier
    //   in the same session can shadow or conflict with the bulk UPDATE result.
    //
    // Returns int so callers can log/assert how many rows were actually updated.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken r
            SET r.revoked = true
            WHERE r.userEmail = :email
            AND r.revoked = false
            """)
    int revokeAllByUserEmail(@Param("email") String email);

    // Derived delete — Spring Data generates the correct DELETE SQL.
    // @Modifying is required because it is a DML operation.
    @Modifying
    void deleteByExpiresAtBefore(Instant now);

    // Native SQL COUNT — bypasses the Hibernate L1 identity cache ENTIRELY.
    // Used in integration tests to assert the post-UPDATE DB state without
    // needing @Transactional, entityManager.refresh(), or entityManager.clear().
    // Unlike JPQL queries, native queries never return managed entity objects,
    // so there is nothing in the cache to serve stale results from.
    @Query(
        value = "SELECT COUNT(*) FROM refresh_tokens WHERE user_email = :email AND revoked = false",
        nativeQuery = true
    )
    long countActiveTokensByUserEmail(@Param("email") String email);
}