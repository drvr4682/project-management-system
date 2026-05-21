package com.pms.apigateway.integration;

import com.pms.common.security.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

        validAdminToken = generateToken(
                "admin@test.com",
                "ADMIN"
        );

        validUserToken = generateToken(
                "user@test.com",
                "USER"
        );
    }

    private String generateToken(String email, String role) {

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
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
    @DisplayName("Public endpoint should be accessible without token")
    void loginEndpointShouldBePublic() throws Exception {

        mockMvc.perform(post("/test/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("PUBLIC SUCCESS"));
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

        assertTrue(jwtUtil.validateToken(validAdminToken),
                "A valid ADMIN token should pass validation");
    }

    @Test
    @DisplayName("USER role should not access admin endpoint")
    void userRoleShouldNotAccessAdminRoutes() throws Exception {

        mockMvc.perform(get("/test/admin")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + validUserToken
                        ))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role should access admin endpoint")
    void adminRoleShouldAccessAdminRoutes() throws Exception {

        mockMvc.perform(get("/test/admin")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + validAdminToken
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMIN SUCCESS"));
    }

    @Test
    @DisplayName("Invalid JWT token should return 401")
    void invalidJwtShouldReturn401() throws Exception {

        mockMvc.perform(
                        get("/api/v1/projects")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer this.is.invalid"
                                )
                )
                .andExpect(status().isUnauthorized());
    }
}