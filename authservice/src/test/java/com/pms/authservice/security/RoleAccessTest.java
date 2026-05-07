package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RoleAccessTest {

    @Test
    void contextLoads() {
        // Ensures RBAC config loads
    }
}