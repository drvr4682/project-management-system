package com.pms.authservice.repository;

import com.pms.authservice.entity.PasswordResetToken;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Transactional
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String email) {
        return userRepository.save(User.builder()
                .userName("Test User")
                .email(email)
                .password("Password@123")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build());
    }

    @Test
    @DisplayName("A. Should find password reset token by token value")
    void shouldFindPasswordResetTokenByToken() {
        User user = createTestUser("findbytoken@test.com");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("uuid-token-findbytoken")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token);

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("uuid-token-findbytoken");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("findbytoken@test.com");
        assertThat(found.get().isUsed()).isFalse();
    }

    @Test
    @DisplayName("B. Should find tokens by user ID")
    void shouldFindTokensByUserId() {
        User user = createTestUser("findbyuserid@test.com");

        PasswordResetToken token1 = PasswordResetToken.builder()
                .token("token-1")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        PasswordResetToken token2 = PasswordResetToken.builder()
                .token("token-2")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .build();

        passwordResetTokenRepository.save(token1);
        passwordResetTokenRepository.save(token2);

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserId(user.getId());
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(PasswordResetToken::getToken).containsExactlyInAnyOrder("token-1", "token-2");
    }

    @Test
    @DisplayName("C. Should invalidate all unused tokens for a specific user ID")
    void shouldInvalidateUnusedTokensByUserId() {
        User user = createTestUser("invalidate@test.com");

        PasswordResetToken token1 = PasswordResetToken.builder()
                .token("token-1")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        PasswordResetToken token2 = PasswordResetToken.builder()
                .token("token-2")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        PasswordResetToken token3 = PasswordResetToken.builder()
                .token("token-3")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .build();

        passwordResetTokenRepository.save(token1);
        passwordResetTokenRepository.save(token2);
        passwordResetTokenRepository.save(token3);

        int updatedCount = passwordResetTokenRepository.invalidateUnusedTokensByUserId(user.getId());
        assertThat(updatedCount).isEqualTo(2);

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserId(user.getId());
        assertThat(tokens).extracting(PasswordResetToken::isUsed).containsOnly(true);
    }

    @Test
    @DisplayName("D. Should delete expired or used tokens older than a specific limit")
    void shouldDeleteExpiredOrUsedTokensOlderThan() {
        User user = createTestUser("pruner@test.com");
        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        // 1. Expired unused token older than 30 days -> SHOULD be deleted
        PasswordResetToken token1 = PasswordResetToken.builder()
                .token("expired-old-token")
                .user(user)
                .expiryDate(limit.minusMinutes(1))
                .createdAt(limit.minusMinutes(10))
                .used(false)
                .build();

        // 2. Used token older than 30 days -> SHOULD be deleted
        PasswordResetToken token2 = PasswordResetToken.builder()
                .token("used-old-token")
                .user(user)
                .expiryDate(limit.plusHours(1))
                .createdAt(limit.minusMinutes(1))
                .used(true)
                .build();

        // 3. Active fresh unused token -> SHOULD NOT be deleted
        PasswordResetToken token3 = PasswordResetToken.builder()
                .token("active-fresh-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now())
                .used(false)
                .build();

        // 4. Used fresh token -> SHOULD NOT be deleted
        PasswordResetToken token4 = PasswordResetToken.builder()
                .token("used-fresh-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .used(true)
                .build();

        passwordResetTokenRepository.save(token1);
        passwordResetTokenRepository.save(token2);
        passwordResetTokenRepository.save(token3);
        passwordResetTokenRepository.save(token4);

        int deletedCount = passwordResetTokenRepository.deleteExpiredOrUsedTokensOlderThan(limit);
        assertThat(deletedCount).isEqualTo(2);

        List<PasswordResetToken> remaining = passwordResetTokenRepository.findAll();
        assertThat(remaining).hasSize(2);
        assertThat(remaining).extracting(PasswordResetToken::getToken)
                .containsExactlyInAnyOrder("active-fresh-token", "used-fresh-token");
    }

    @Test
    @DisplayName("E. Should enforce unique token constraint")
    void shouldEnforceUniqueTokenConstraint() {
        User user1 = createTestUser("user1@test.com");
        User user2 = createTestUser("user2@test.com");

        PasswordResetToken token1 = PasswordResetToken.builder()
                .token("same-token-value")
                .user(user1)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token1);

        PasswordResetToken token2 = PasswordResetToken.builder()
                .token("same-token-value")
                .user(user2)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        assertThatThrownBy(() -> {
            passwordResetTokenRepository.saveAndFlush(token2);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("F. Should handle used=true lifecycle behavior correctly")
    void shouldHandleUsedLifecycleBehavior() {
        User user = createTestUser("lifecycle@test.com");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("lifecycle-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        PasswordResetToken saved = passwordResetTokenRepository.saveAndFlush(token);
        assertThat(saved.isUsed()).isFalse();

        saved.setUsed(true);
        PasswordResetToken updated = passwordResetTokenRepository.saveAndFlush(saved);
        assertThat(updated.isUsed()).isTrue();

        Optional<PasswordResetToken> fetched = passwordResetTokenRepository.findByToken("lifecycle-token");
        assertThat(fetched).isPresent();
        assertThat(fetched.get().isUsed()).isTrue();
    }
}
