package com.pms.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String TEST_SECRET =
            "testsecretkeytestsecretkeytestsecret12";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {

        jwtUtil = new JwtUtil();

        java.lang.reflect.Field secretField =
                JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, TEST_SECRET);

        jwtUtil.init();
    }

    @Test
    void shouldValidateToken() {

        String token =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void shouldExtractUsername() {

        String token =
                Jwts.builder()
                        .setSubject("user@test.com")
                        .claim("role", "USER")
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        assertEquals("user@test.com", jwtUtil.extractUsername(token));
    }

    @Test
    void shouldExtractRole() {

        String token =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void shouldRejectExpiredToken() {

        String expiredToken =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                        .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void shouldRejectTamperedToken() {

        String token =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        // Tamper with the token by flipping a character in the signature
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtUtil.validateToken(tamperedToken));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {

        assertFalse(jwtUtil.validateToken("this.is.not.a.valid.jwt"));
    }
}