package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testValidateToken() {

        String email = "test@example.com";

        String token = jwtUtil.generateToken(email, "USER");

        assertNotNull(token);

        String extractedEmail = jwtUtil.extractUsername(token);
        String extractedRole = jwtUtil.extractRole(token);

        assertEquals(email, extractedEmail);
        assertEquals("USER", extractedRole);
    }
}