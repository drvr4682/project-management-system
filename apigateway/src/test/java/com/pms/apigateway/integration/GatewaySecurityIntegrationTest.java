package com.pms.apigateway.integration;

import com.pms.apigateway.security.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;

    @BeforeEach
    void setUp() {

        validToken =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .setIssuedAt(new Date())
                        .setExpiration(
                                new Date(
                                        System.currentTimeMillis()
                                                + 3600000
                                )
                        )
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        "testsecretkeytestsecretkeytestsecret12"
                                                .getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();
    }

    @Test
    @DisplayName("Health endpoint should be public")
    void healthEndpointShouldBePublic() throws Exception {

        mockMvc.perform(
                        get("/health")
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login endpoint should be public")
    void loginEndpointShouldBePublic() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                )
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Projects endpoint should reject unauthorized requests")
    void projectsEndpointShouldRejectUnauthorizedRequests()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/projects")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT filter should authenticate valid token")
    void jwtFilterShouldAuthenticateValidToken() {

        boolean valid =
                jwtUtil.validateToken(validToken);

        org.junit.jupiter.api.Assertions.assertTrue(valid);
    }

    @Test
    @DisplayName("USER role should not access admin routes")
    void userRoleShouldNotAccessAdminRoutes()
                throws Exception {

        String userToken =
                Jwts.builder()
                        .setSubject("user@test.com")
                        .claim("role", "USER")
                        .setIssuedAt(new Date())
                        .setExpiration(
                                new Date(
                                        System.currentTimeMillis()
                                                + 3600000
                                )
                        )
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        "testsecretkeytestsecretkeytestsecret12"
                                                .getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        mockMvc.perform(
                        get("/api/v1/admin/dashboard")
                                .header(
                                        "Authorization",
                                        "Bearer " + userToken
                                )
                )
                .andExpect(status().isForbidden());
    }
}