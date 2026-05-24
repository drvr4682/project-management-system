package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.repository.UserRepository;
import com.pms.common.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository       userRepository       = Mockito.mock(UserRepository.class);
    private final PasswordEncoder      passwordEncoder      = Mockito.mock(PasswordEncoder.class);
    private final JwtUtil              jwtUtil              = Mockito.mock(JwtUtil.class);
    private final AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
    private final RefreshTokenService  refreshTokenService  = Mockito.mock(RefreshTokenService.class);

    private final AuthService authService = 
            new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil, authenticationManager, refreshTokenService);

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@123")).thenReturn("hashed");

        User savedUser = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@mail.com", response.getEmail());
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("exists@mail.com");
        request.setPassword("Test@123");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail("exists@mail.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    void shouldLoginSuccessfullyAndReturnBothTokens() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken("test@mail.com")).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("USER", response.getRole());
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken(),
                "Login response must now include a refresh token");
    }

    @Test
    void shouldNormalizeEmailToLowercaseOnLogin() {

        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@MAIL.COM  ");
        request.setPassword("Test@123");

        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(anyString())).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("test@mail.com", response.getEmail());
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    void shouldRevokeAllRefreshTokensOnLogout() {

        authService.logout("user@test.com");

        verify(refreshTokenService).revokeAll("user@test.com");
    }
}