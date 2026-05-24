package com.pms.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.entity.Role;
import com.pms.authservice.security.CustomUserDetailsService;
import com.pms.authservice.service.AuthService;
import com.pms.authservice.service.RefreshTokenService;
import com.pms.common.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    // Required by Spring Security auto-configuration in @WebMvcTest
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/register
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowRegisterWithoutAuth() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@test.com");
        request.setPassword("Test@123");
        request.setRole(Role.USER);

        RegisterResponse mockResponse = RegisterResponse.builder()
                .id(1L)
                .name("Test")
                .email("test@test.com")
                .role("USER")
                .build();

        Mockito.when(authService.register(Mockito.any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void shouldFailRegisterWithInvalidInput() throws Exception {

        RegisterRequest request = new RegisterRequest(); // all fields blank

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailRegisterWithWeakPassword() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@test.com");
        request.setPassword("weakpassword"); // no uppercase, number or special char
        request.setRole(Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/login
    // -------------------------------------------------------------------------

    @Test
    void shouldLoginAndReturnBothAccessAndRefreshTokens() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("Test@123");

        LoginResponse mockResponse = LoginResponse.builder()
                .token("access-token")
                .refreshToken("refresh-token")
                .email("user@test.com")
                .role("USER")
                .build();

        Mockito.when(authService.login(Mockito.any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/refresh
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnNewTokensOnValidRefresh() throws Exception {

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        RefreshTokenResponse mockResponse = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .email("user@test.com")
                .role("USER")
                .build();

        Mockito.when(authService.refresh(Mockito.any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldReturn400WhenRefreshTokenIsBlank() throws Exception {

        RefreshTokenRequest request = new RefreshTokenRequest();
        // refreshToken field intentionally left blank — @NotBlank should reject it

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/logout
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn200OnSuccessfulLogout() throws Exception {

        Mockito.doNothing().when(authService).logout(Mockito.any(), Mockito.any());

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer some-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}