package com.pms.common.security;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the canonical {@link JwtUtil}.
 *
 * <p>Uses the test constructor — no Spring context needed.
 * Replaces and consolidates the four per-service JwtUtilTest classes.
 */
class JwtUtilTest {

    /** Must be at least 32 characters for HS256. */
    private static final String SECRET =
            "test-secret-key-must-be-at-least-32-chars-long";
    private static final long EXPIRATION = 86_400_000L; // 24 h

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    // -------------------------------------------------------------------------
    // generateToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateToken returns a non-blank JWT string")
    void generateToken_returnsNonBlankJwt() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(token).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // extractUsername / extractRole / extractAllClaims
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractUsername returns the subject set during generation")
    void extractUsername_returnsSubject() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("extractRole returns the role claim set during generation")
    void extractRole_returnsRoleClaim() {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractAllClaims returns subject and role claims")
    void extractAllClaims_returnsBothClaims() {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN");
        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("admin@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractAllClaims throws on a tampered token")
    void extractAllClaims_tamperedToken_throwsException() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtUtil.extractAllClaims(tampered))
                .isInstanceOf(Exception.class);
    }

    // -------------------------------------------------------------------------
    // validateToken
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validateToken returns true for a fresh, valid token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a completely invalid string")
    void validateToken_invalidString_returnsFalse() {
        assertThat(jwtUtil.validateToken("this.is.not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for an already-expired token")
    void validateToken_expiredToken_returnsFalse() {
        // Negative expiration → token is born expired
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1_000L);
        String token = expiredJwtUtil.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a token signed with a different secret")
    void validateToken_wrongSecret_returnsFalse() {
        JwtUtil differentKey = new JwtUtil(
                "different-secret-key-at-least-32-chars-!", EXPIRATION);
        String tokenFromOtherKey = differentKey.generateToken("user@example.com", "USER");
        assertThat(jwtUtil.validateToken(tokenFromOtherKey)).isFalse();
    }
}
