package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class JwtRoleTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldContainAdminRoleInToken() {

        String token = jwtUtil.generateToken("admin@test.com", "ADMIN");

        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void shouldContainUserRoleInToken() {

        String token = jwtUtil.generateToken("user@test.com", "USER");

        assertEquals("USER", jwtUtil.extractRole(token));
    }
}