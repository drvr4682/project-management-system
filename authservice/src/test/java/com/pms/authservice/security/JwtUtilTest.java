package com.pms.authservice.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void shouldGenerateAndValidateToken() {

        String token = jwtUtil.generateToken("test@mail.com");

        assertNotNull(token);
        assertEquals("test@mail.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token, "test@mail.com"));
    }
}