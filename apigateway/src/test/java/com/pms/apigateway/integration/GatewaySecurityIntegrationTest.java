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

    private static final String TEST_SECRET =
            "testsecretkeytestsecretkeytestsecret12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    private String validAdminToken;
    private String validUserToken;

    @BeforeEach
    void setUp() {

        validAdminToken =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        validUserToken =
                Jwts.builder()
                        .setSubject("user@test.com")
                        .claim("role", "USER")
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();
    }

    @Test
    @DisplayName("Health endpoint should be public and return 200")
    void healthEndpointShouldBePublic() throws Exception {

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login endpoint should be public — returns 5xx because auth service is not running in test")
    void loginEndpointShouldBePublic() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Projects endpoint should return 401 when no JWT is provided")
    void projectsEndpointShouldRejectUnauthorizedRequests() throws Exception {

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT filter should validate a correctly signed token")
    void jwtFilterShouldAuthenticateValidToken() {

        boolean valid = jwtUtil.validateToken(validAdminToken);

        org.junit.jupiter.api.Assertions.assertTrue(valid,
                "A valid ADMIN token should pass validation");
    }

    @Test
    @DisplayName("USER role should be forbidden from ADMIN routes")
    void userRoleShouldNotAccessAdminRoutes() throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/dashboard")
                                .header("Authorization", "Bearer " + validUserToken)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role should be allowed through to admin routes (5xx = service down, not security block)")
    void adminRoleShouldAccessAdminRoutes() throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/dashboard")
                                .header("Authorization", "Bearer " + validAdminToken)
                )
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Invalid JWT token should return 401")
    void invalidJwtShouldReturn401() throws Exception {

        mockMvc.perform(
                        get("/api/v1/projects")
                                .header("Authorization", "Bearer this.is.invalid")
                )
                .andExpect(status().isUnauthorized());
    }
}