package com.pms.authservice.exception;

import com.pms.authservice.controller.AuthController;
import com.pms.authservice.security.CustomUserDetailsService;
import com.pms.authservice.security.JwtUtil;
import com.pms.authservice.service.AuthService;

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
class ExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldReturn400_whenEmailAlreadyExists() throws Exception {
        Mockito.when(authService.register(Mockito.any()))
                .thenThrow(new UserAlreadyExistsException("Email already registered"));

        String body = """
                {
                  "name": "Test",
                  "email": "existing@test.com",
                  "password": "password123",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturn401_whenBadCredentials() throws Exception {
        Mockito.when(authService.login(Mockito.any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        String body = """
                {
                  "email": "user@test.com",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn500_whenUnexpectedExceptionThrown() throws Exception {
        Mockito.when(authService.register(Mockito.any()))
                .thenThrow(new RuntimeException("Unexpected DB failure"));

        String body = """
                {
                  "name": "Test",
                  "email": "test@test.com",
                  "password": "password123",
                  "role": "USER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }
}