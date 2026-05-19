package com.pms.taskservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            "test-secret-key-must-be-at-least-32-characters-long-for-hs256";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_andExtractUsername_success() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void generateToken_andExtractRole_success() {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("invalid.jwt.token")).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000L); // already expired
        String token = expiredJwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void extractAllClaims_withTamperedToken_throwsException() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtUtil.extractAllClaims(tampered))
                .isInstanceOf(Exception.class);
    }
}
