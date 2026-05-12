package com.pms.projectservice.security;

import io.jsonwebtoken.Claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private final String SECRET =
            "testsecretkeytestsecretkeytest12";

    @BeforeEach
    void setUp() {

        jwtUtil = new JwtUtil(
                SECRET,
                3600000L
        );
    }

    @Test
    @DisplayName("Should generate token successfully")
    void shouldGenerateTokenSuccessfully() {

        String token =
                jwtUtil.generateToken(
                        "admin@test.com",
                        "ADMIN"
                );

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {

        String token =
                jwtUtil.generateToken(
                        "user@test.com",
                        "USER"
                );

        String username =
                jwtUtil.extractUsername(token);

        assertEquals("user@test.com", username);
    }

    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRoleFromToken() {

        String token =
                jwtUtil.generateToken(
                        "admin@test.com",
                        "ADMIN"
                );

        String role =
                jwtUtil.extractRole(token);

        assertEquals("ADMIN", role);
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {

        String token =
                jwtUtil.generateToken(
                        "admin@test.com",
                        "ADMIN"
                );

        boolean valid =
                jwtUtil.validateToken(token);

        assertTrue(valid);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {

        String invalidToken =
                "invalid.jwt.token";

        boolean valid =
                jwtUtil.validateToken(invalidToken);

        assertFalse(valid);
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken()
            throws InterruptedException {

        JwtUtil shortExpiryJwtUtil =
                new JwtUtil(
                        SECRET,
                        1L
                );

        String token =
                shortExpiryJwtUtil.generateToken(
                        "admin@test.com",
                        "ADMIN"
                );

        Thread.sleep(5);

        boolean valid =
                shortExpiryJwtUtil.validateToken(token);

        assertFalse(valid);
    }

    @Test
    @DisplayName("Should extract all claims")
    void shouldExtractAllClaims() {

        String token =
                jwtUtil.generateToken(
                        "admin@test.com",
                        "ADMIN"
                );

        Claims claims =
                jwtUtil.extractAllClaims(token);

        assertEquals(
                "admin@test.com",
                claims.getSubject()
        );

        assertEquals(
                "ADMIN",
                claims.get("role")
        );
    }
}