package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class JwtRoleTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldContainRoleInToken() {

        String token = jwtUtil.generateToken("test@mail.com", "ADMIN");

        String role = jwtUtil.extractRole(token);

        assertEquals("ADMIN", role);
    }
}