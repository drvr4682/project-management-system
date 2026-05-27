package com.pms.authservice.repository;

import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class VerificationTokenRepositoryTest {

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and find verification token")
    void shouldSaveAndFindVerificationToken() {

        User user = userRepository.save(
                User.builder()
                        .userName("john")
                        .email("john@example.com")
                        .password("password")
                        .role(Role.USER)
                        .enabled(false)
                        .emailVerified(false)
                        .build()
        );

        VerificationToken token = VerificationToken.builder()
                .token("sample-uuid-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        verificationTokenRepository.save(token);

        Optional<VerificationToken> savedToken =
                verificationTokenRepository.findByToken("sample-uuid-token");

        assertThat(savedToken).isPresent();

        assertThat(savedToken.get().getUser().getEmail())
                .isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should find token by user id")
    void shouldFindTokenByUserId() {

        User user = userRepository.save(
                User.builder()
                        .userName("mike")
                        .email("mike@example.com")
                        .password("password")
                        .role(Role.USER)
                        .enabled(false)
                        .emailVerified(false)
                        .build()
        );

        VerificationToken token = VerificationToken.builder()
                .token("another-token")
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        verificationTokenRepository.save(token);

        List<VerificationToken> result =
                verificationTokenRepository.findByUserId(user.getId());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getToken())
                .isEqualTo("another-token");
    }
}