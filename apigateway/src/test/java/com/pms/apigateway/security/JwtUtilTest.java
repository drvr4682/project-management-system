package com.pms.apigateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {

        jwtUtil = new JwtUtil();

        java.lang.reflect.Field secretField =
                JwtUtil.class.getDeclaredField("secret");

        secretField.setAccessible(true);

        secretField.set(
                jwtUtil,
                "testsecretkeytestsecretkeytest12"
        );

        jwtUtil.init();
    }

    @Test
    void shouldValidateToken() {

        String token =
                io.jsonwebtoken.Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .signWith(
                                io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                        "testsecretkeytestsecretkeytest12".getBytes()
                                )
                        )
                        .compact();

        assertTrue(jwtUtil.validateToken(token));
    }
}