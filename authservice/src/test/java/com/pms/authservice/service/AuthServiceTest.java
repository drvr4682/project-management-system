package com.pms.authservice.service;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import com.pms.authservice.exception.EmailVerificationException;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.service.email.EmailService;
import com.pms.authservice.service.verification.VerificationService;
import com.pms.common.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository              userRepository              = Mockito.mock(UserRepository.class);
    private final VerificationService         verificationService         = Mockito.mock(VerificationService.class);
    private final PasswordEncoder             passwordEncoder             = Mockito.mock(PasswordEncoder.class);
    private final JwtUtil                     jwtUtil                     = Mockito.mock(JwtUtil.class);
    private final AuthenticationManager       authenticationManager       = Mockito.mock(AuthenticationManager.class);
    private final RefreshTokenService         refreshTokenService         = Mockito.mock(RefreshTokenService.class);

    // AuthServiceImpl now depends on StringRedisTemplate for access-token blocklisting on logout.
    private final StringRedisTemplate         redisTemplate               = Mockito.mock(StringRedisTemplate.class);
    private final EmailService                emailService                = Mockito.mock(EmailService.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps                = Mockito.mock(ValueOperations.class);

    private final AuthService authService = new AuthServiceImpl(
            userRepository, verificationService, passwordEncoder, jwtUtil,
            authenticationManager, refreshTokenService, redisTemplate, emailService);

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

        VerificationToken mockToken = VerificationToken.builder()
                .token("dummy-token")
                .user(savedUser)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(verificationService.createVerificationToken(any(User.class))).thenReturn(mockToken);

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@mail.com", response.getEmail());

        verify(verificationService, times(1)).createVerificationToken(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(eq("test@mail.com"), eq("Test"), anyString());
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
                .emailVerified(true)
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
                "Login response must include a refresh token");
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
                .emailVerified(true)
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

        authService.logout("user@test.com", null);

        verify(refreshTokenService).revokeAll("user@test.com");
    }

    @Test
    void shouldBlocklistAccessTokenInRedisOnLogout() {

        String accessToken = "some.jwt.token";
        String jti         = "test-jti-123";

        // Stub JwtUtil so the blocklist path executes
        when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        when(jwtUtil.extractJti(accessToken)).thenReturn(jti);
        when(jwtUtil.getExpirationMillis(accessToken)).thenReturn(300_000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.logout("user@test.com", accessToken);

        // Refresh tokens must be revoked in DB
        verify(refreshTokenService).revokeAll("user@test.com");

        // Access token JTI must be written to Redis blocklist
        verify(valueOps).set(
                eq("blocklist:jti:" + jti),
                eq("revoked"),
                eq(300_000L),
                any()
        );
    }

    @Test
    void shouldStillLogoutEvenIfRedisWriteFails() {

        String accessToken = "some.jwt.token";

        when(jwtUtil.validateToken(accessToken)).thenReturn(true);
        when(jwtUtil.extractJti(accessToken)).thenReturn("jti-123");
        when(jwtUtil.getExpirationMillis(accessToken)).thenReturn(300_000L);
        // Redis blows up
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        // Should not throw — logout must complete even if Redis fails
        assertDoesNotThrow(() -> authService.logout("user@test.com", accessToken));

        // Refresh tokens must still be revoked in DB
        verify(refreshTokenService).revokeAll("user@test.com");
    }

    // -------------------------------------------------------------------------
    // email verification
    // -------------------------------------------------------------------------

    @Test
    void shouldGenerateVerificationToken() {
        // Registering a user successfully generates the token via VerificationService
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

        VerificationToken mockToken = VerificationToken.builder()
                .token("generated-uuid-token")
                .user(savedUser)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(verificationService.createVerificationToken(any(User.class))).thenReturn(mockToken);

        authService.register(request);

        verify(verificationService, times(1)).createVerificationToken(any(User.class));
    }

    @Test
    void shouldSendVerificationEmail() {
        // Registering a user successfully sends the verification email
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

        VerificationToken mockToken = VerificationToken.builder()
                .token("generated-uuid-token")
                .user(savedUser)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(verificationService.createVerificationToken(any(User.class))).thenReturn(mockToken);

        authService.register(request);

        verify(emailService, times(1)).sendVerificationEmail(eq("test@mail.com"), eq("Test"), anyString());
    }

    @Test
    void shouldBlockLoginBeforeVerification() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(false) // Unverified!
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);

        assertThrows(EmailVerificationException.class, () -> authService.login(request));
    }

    @Test
    void shouldAllowLoginAfterVerification() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(true) // Verified!
                .enabled(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken("test@mail.com")).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getToken());
    }

    @Test
    void shouldVerifyUserSuccessfully() {
        String token = "valid-token-uuid";
        authService.verifyEmail(token);
        verify(verificationService, times(1)).validateVerificationToken(token);
    }

    @Test
    void shouldRejectExpiredVerificationToken() {
        String token = "expired-token-uuid";
        doThrow(new com.pms.authservice.exception.ExpiredVerificationTokenException("Verification link has expired"))
                .when(verificationService).validateVerificationToken(token);

        assertThrows(com.pms.authservice.exception.ExpiredVerificationTokenException.class, 
                () -> authService.verifyEmail(token));
    }

    @Test
    void shouldRejectInvalidVerificationToken() {
        String token = "invalid-token-uuid";
        doThrow(new com.pms.authservice.exception.InvalidVerificationTokenException("Invalid verification token"))
                .when(verificationService).validateVerificationToken(token);

        assertThrows(com.pms.authservice.exception.InvalidVerificationTokenException.class, 
                () -> authService.verifyEmail(token));
    }

    @Test
    void shouldThrowWhenLoginDisabled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        org.springframework.security.core.AuthenticationException disabledEx = 
                new org.springframework.security.authentication.DisabledException("User is disabled");
        when(authenticationManager.authenticate(any())).thenThrow(disabledEx);

        assertThrows(org.springframework.security.core.AuthenticationException.class, () -> authService.login(request));
    }

    @Test
    void shouldResendVerificationEmailSuccessfully() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@mail.com");

        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .emailVerified(false)
                .build();

        VerificationToken mockToken = VerificationToken.builder()
                .token("new-token")
                .user(user)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(verificationService.createVerificationToken(user)).thenReturn(mockToken);

        authService.resendVerificationEmail(request);

        verify(verificationService, times(1)).createVerificationToken(user);
        verify(emailService, times(1)).sendVerificationEmail(eq("test@mail.com"), eq("Test"), anyString());
    }

    @Test
    void shouldRejectResendVerificationEmailIfAlreadyVerified() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@mail.com");

        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        assertThrows(EmailVerificationException.class, () -> authService.resendVerificationEmail(request));

        verify(verificationService, never()).createVerificationToken(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectResendVerificationEmailIfCooldownActive() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("cooldown@mail.com");

        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("cooldown@mail.com")
                .emailVerified(false)
                .build();

        VerificationToken mockToken = VerificationToken.builder()
                .token("new-token")
                .user(user)
                .build();

        when(userRepository.findByEmail("cooldown@mail.com")).thenReturn(Optional.of(user));
        when(verificationService.createVerificationToken(user)).thenReturn(mockToken);

        // First attempt succeeds and sets cooldown
        authService.resendVerificationEmail(request);

        // Second attempt fails due to active cooldown
        assertThrows(EmailVerificationException.class, () -> authService.resendVerificationEmail(request));
    }
}