package com.pms.authservice.repository;

import com.pms.authservice.entity.RefreshToken;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    private User createTestUser(String email, String firstName, String surname) {
        return userRepository.save(User.builder()
                .firstName(firstName)
                .surname(surname)
                .email(email)
                .password("hashed")
                .role(Role.USER)
                .build());
    }

    @BeforeEach
    void setUp() {
        savedUser = createTestUser("test@example.com", "Test", "User");
    }

    // -------------------------------------------------------------------------
    // findByToken
    // -------------------------------------------------------------------------

    @Test
    void shouldFindRefreshTokenByToken() {

        String tokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(RefreshToken.builder()
                .token(tokenValue)
                .userId(savedUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);

        assertTrue(found.isPresent());
        assertEquals(tokenValue, found.get().getToken());
        assertEquals(savedUser.getId(), found.get().getUserId());
    }

    @Test
    void shouldReturnEmptyWhenTokenNotFound() {

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("non-existent-token");

        assertFalse(found.isPresent());
    }

    // -------------------------------------------------------------------------
    // revokeAllByUserId
    // -------------------------------------------------------------------------

    // @Transactional here ensures the bulk JPQL UPDATE and the subsequent
    // findAll() share one session.  clearAutomatically = true on the repository
    // method evicts the affected rows from the first-level cache so findAll()
    // returns the fresh DB state — not the pre-update in-memory snapshot.
    @Test
    @Transactional
    void shouldRevokeAllActiveTokensForUser() {

        refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(savedUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(savedUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        int updatedCount =
                refreshTokenRepository.revokeAllByUserId(savedUser.getId());

        assertEquals(2, updatedCount);

        refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUserId().equals(savedUser.getId()))
                .forEach(token -> assertTrue(
                        token.isRevoked(),
                        "All tokens for user should be revoked"
                ));
    }

    @Test
    void shouldNotRevokeTokensForOtherUsers() {

        User otherUser = createTestUser("other@example.com", "Other", "User");

        String otherToken = UUID.randomUUID().toString();

        refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(savedUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        refreshTokenRepository.save(RefreshToken.builder()
                .token(otherToken)
                .userId(otherUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        refreshTokenRepository.revokeAllByUserId(savedUser.getId());

        Optional<RefreshToken> other = refreshTokenRepository.findByToken(otherToken);
        assertTrue(other.isPresent());
        assertFalse(other.get().isRevoked(), "Other user's token should NOT be revoked");
    }

    @Test
    void shouldNotRevokeAlreadyRevokedTokensAgain() {

        String tokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(RefreshToken.builder()
                .token(tokenValue)
                .userId(savedUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build());

        assertDoesNotThrow(() ->
                refreshTokenRepository.revokeAllByUserId(savedUser.getId()));
    }

    // -------------------------------------------------------------------------
    // deleteAllExpired
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteExpiredTokens() {

        refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(savedUser.getId())
                .expiresAt(Instant.now().minusSeconds(60))
                .revoked(false)
                .build());

        String activeTokenValue = UUID.randomUUID().toString();

        refreshTokenRepository.save(RefreshToken.builder()
                .token(activeTokenValue)
                .userId(savedUser.getId())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build());

        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());

        Optional<RefreshToken> active = refreshTokenRepository.findByToken(activeTokenValue);

        assertTrue(active.isPresent(), "Active token should survive cleanup");
        assertEquals(1, refreshTokenRepository.count(), "Only the active token should remain");
    }
}