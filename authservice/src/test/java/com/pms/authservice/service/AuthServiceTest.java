package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.exception.UserNotFoundException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final JwtUtil jwtUtil = Mockito.mock(JwtUtil.class);
    private final AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);

    private final AuthService authService =
            new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil, authenticationManager);

    // ── register — success ────────────────────────────────────────────────────

    @Test
    void register_shouldSaveUserAndReturnResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("Test@Mail.COM"); // uppercase to verify normalisation
        request.setPassword("password123");
        request.setRole(Role.USER);

        // The service normalises email before calling existsByEmail
        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        User savedUser = User.builder()
                .id(1L).name("Test User")
                .email("test@mail.com").password("hashed").role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@mail.com", response.getEmail()); // normalised email saved
        assertEquals("USER", response.getRole());

        // Verify email normalisation was applied before save
        verify(userRepository, times(1)).existsByEmail("test@mail.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ── register — duplicate email ────────────────────────────────────────────

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("existing@mail.com");
        request.setPassword("password");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail("existing@mail.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));

        // User must NOT be saved when email is duplicate
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void register_shouldNormaliseEmailBeforeCheckingDuplicate() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("  UPPER@MAIL.COM  "); // leading spaces + uppercase
        request.setPassword("password");
        request.setRole(Role.USER);

        // Must be called with normalised version
        when(userRepository.existsByEmail("upper@mail.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository).existsByEmail("upper@mail.com");
        verify(userRepository, never()).save(any());
    }

    // ── login — success ───────────────────────────────────────────────────────

    @Test
    void login_shouldReturnTokenAndRole_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L).email("test@mail.com")
                .password("hashed").role(Role.USER)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("test@mail.com", "USER")).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("USER", response.getRole());
        assertEquals("test@mail.com", response.getEmail());
    }

    // ── login — wrong password ────────────────────────────────────────────────

    @Test
    void login_shouldThrow_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        // User lookup and token generation must never happen
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    // ── login — user not found ────────────────────────────────────────────────

    @Test
    void login_shouldThrow_whenUserNotFoundAfterAuth() {
        // This is an edge case: auth passes but DB lookup fails
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@mail.com");
        request.setPassword("password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(request));

        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    // ── login — email normalisation ───────────────────────────────────────────

    @Test
    void login_shouldNormaliseEmail_beforeLookup() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@MAIL.COM  ");
        request.setPassword("password");

        User user = User.builder()
                .id(1L).email("test@mail.com")
                .password("hashed").role(Role.ADMIN)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("test@mail.com", "ADMIN")).thenReturn("admin-token");

        LoginResponse response = authService.login(request);

        assertEquals("admin-token", response.getToken());
        verify(userRepository).findByEmail("test@mail.com");
    }

    // ── userExists ────────────────────────────────────────────────────────────

    @Test
    void userExists_shouldReturnTrue_whenEmailExists() {
        when(userRepository.existsByEmail("found@mail.com")).thenReturn(true);
        assertTrue(authService.userExists("found@mail.com"));
    }

    @Test
    void userExists_shouldReturnFalse_whenEmailNotFound() {
        when(userRepository.existsByEmail("missing@mail.com")).thenReturn(false);
        assertFalse(authService.userExists("missing@mail.com"));
    }

    @Test
    void userExists_shouldNormaliseEmail() {
        when(userRepository.existsByEmail("test@mail.com")).thenReturn(true);
        assertTrue(authService.userExists("  TEST@MAIL.COM  "));
        verify(userRepository).existsByEmail("test@mail.com");
    }
}