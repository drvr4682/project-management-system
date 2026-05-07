package com.pms.authservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Test
    void contextLoads() {
        // Just verifies Spring context loads with security config
    }
}