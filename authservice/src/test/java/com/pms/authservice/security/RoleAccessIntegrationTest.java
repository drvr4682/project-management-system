package com.pms.authservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")                  // activates TestController
@TestPropertySource(locations = "classpath:application.properties")
class RoleAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Test
    void adminShouldAccessBothAdminAndUserEndpoints() throws Exception {

        String token = jwtUtil.generateToken("admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Gateway-Secret", gatewaySecret))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/test/user")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Gateway-Secret", gatewaySecret))
                .andExpect(status().isOk());
    }

    @Test
    void userShouldOnlyAccessUserEndpoint() throws Exception {

        String token = jwtUtil.generateToken("user@test.com", "USER");

        mockMvc.perform(get("/api/v1/test/user")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Gateway-Secret", gatewaySecret))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/test/admin")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Gateway-Secret", gatewaySecret))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutTokenShouldBeUnauthorized() throws Exception {

        mockMvc.perform(get("/api/v1/test/user")
                        .header("X-Gateway-Secret", gatewaySecret))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithoutGatewaySecretShouldBeForbidden() throws Exception {

        String token = jwtUtil.generateToken("user@test.com", "USER");

        mockMvc.perform(get("/api/v1/test/user")
                        .header("Authorization", "Bearer " + token))
                // No X-Gateway-Secret → GatewayValidationFilter blocks it
                .andExpect(status().isForbidden());
    }
}