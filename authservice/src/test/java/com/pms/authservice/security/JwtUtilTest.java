package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldGenerateAndValidateToken() {

        String email = "test@example.com";

        String token = jwtUtil.generateToken(email, "USER");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractCorrectUsernameFromToken() {

        String email = "test@example.com";
        String token = jwtUtil.generateToken(email, "USER");

        assertEquals(email, jwtUtil.extractUsername(token));
    }

    @Test
    void shouldExtractCorrectRoleFromToken() {

        String token = jwtUtil.generateToken("test@example.com", "USER");

        assertEquals("USER", jwtUtil.extractRole(token));
    }

    @Test
    void shouldValidateTokenSuccessfully() {

        String email = "test@example.com";
        String token = jwtUtil.generateToken(email, "USER");

        assertTrue(jwtUtil.validateToken(token, email));
    }

    @Test
    void shouldFailValidationForWrongEmail() {

        String token = jwtUtil.generateToken("correct@example.com", "USER");

        assertFalse(jwtUtil.validateToken(token, "wrong@example.com"));
    }
}