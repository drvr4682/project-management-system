package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void testValidateToken() {

        String email = "test@example.com";

        UserDetails userDetails = new User(
                email,
                "password",
                Collections.emptyList()
        );

        String token = jwtUtil.generateToken(email);

        boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertTrue(isValid);
    }
}