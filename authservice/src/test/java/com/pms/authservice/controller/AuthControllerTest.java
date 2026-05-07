package com.pms.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.entity.Role;
import com.pms.authservice.service.AuthService;
import com.pms.authservice.security.JwtUtil;
import com.pms.authservice.security.CustomUserDetailsService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void register_shouldReturn200_withValidBody() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setRole(Role.USER);

        // Return a real response — not null
        Mockito.when(authService.register(Mockito.any()))
                .thenReturn(RegisterResponse.builder()
                        .id(1L)
                        .name("Test User")
                        .email("test@test.com")
                        .role("USER")
                        .build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_shouldReturn400_whenNameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setRole(Role.USER);
        // name is null — validation fails

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400_whenEmailIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("not-an-email");
        request.setPassword("password123");
        request.setRole(Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400_whenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}