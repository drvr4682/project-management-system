package com.pms.authservice.service;

import com.pms.authservice.client.UserFeignClient;
import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import com.pms.authservice.exception.EmailVerificationException;
import com.pms.authservice.exception.UserAlreadyExistsException;
import com.pms.authservice.exception.TooManyRequestsException;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository              userRepository              = Mockito.mock(UserRepository.class);
    private final VerificationService         verificationService         = Mockito.mock(VerificationService.class);
    private final PasswordEncoder             passwordEncoder             = Mockito.mock(PasswordEncoder.class);
    private final JwtUtil                     jwtUtil                     = Mockito.mock(JwtUtil.class);
    private final AuthenticationManager       authenticationManager       = Mockito.mock(AuthenticationManager.class);
    private final RefreshTokenService         refreshTokenService         = Mockito.mock(RefreshTokenService.class);
    private final StringRedisTemplate         redisTemplate               = Mockito.mock(StringRedisTemplate.class);
    private final EmailService                emailService                = Mockito.mock(EmailService.class);
    private final UserFeignClient             userFeignClient             = Mockito.mock(UserFeignClient.class);

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps                = Mockito.mock(ValueOperations.class);

    private final AuthService authService = new AuthServiceImpl(
            userRepository, verificationService, passwordEncoder, jwtUtil,
            authenticationManager, refreshTokenService, redisTemplate, emailService, userFeignClient);

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void shouldRegisterUserSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setSurname("User");
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@123")).thenReturn("hashed");

        UUID userUuid = UUID.randomUUID();
        User savedUser = User.builder()
                .id(userUuid)
                .firstName("Test")
                .surname("User")
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        VerificationToken mockToken = VerificationToken.builder()
                .token("dummy-token")
                .user(savedUser)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(verificationService.createVerificationToken(any(User.class))).thenReturn(mockToken);

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals(userUuid, response.getId());
        assertEquals("test@mail.com", response.getEmail());

        verify(userFeignClient, times(1)).createProfile(any());
        verify(verificationService, times(1)).createVerificationToken(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(eq("test@mail.com"), eq("Test User"), anyString());
    }

    @Test
    void shouldThrowWhenEmailAlreadyRegistered() {

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
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

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(UUID.class))).thenReturn("refresh-token");

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

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(UUID.class))).thenReturn("refresh-token");

        LoginResponse response = authService.login(request);

        assertEquals("test@mail.com", response.getEmail());
    }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    void shouldRevokeAllRefreshTokensOnLogout() {

        UUID logoutId = UUID.randomUUID();
        authService.logout(logoutId.toString(), null);

        verify(refreshTokenService).revokeAll(logoutId);
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

        UUID logoutId = UUID.randomUUID();
        authService.logout(logoutId.toString(), accessToken);

        // Refresh tokens must be revoked in DB
        verify(refreshTokenService).revokeAll(logoutId);

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

        UUID logoutId = UUID.randomUUID();
        // Should not throw — logout must complete even if Redis fails
        assertDoesNotThrow(() -> authService.logout(logoutId.toString(), accessToken));

        // Refresh tokens must still be revoked in DB
        verify(refreshTokenService).revokeAll(logoutId);
    }

    // -------------------------------------------------------------------------
    // email verification
    // -------------------------------------------------------------------------

    @Test
    void shouldGenerateVerificationToken() {
        // Registering a user successfully generates the token via VerificationService
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@123")).thenReturn("hashed");

        UUID userUuid = UUID.randomUUID();
        User savedUser = User.builder()
                .id(userUuid)
                .firstName("Test")
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        VerificationToken mockToken = VerificationToken.builder()
                .token("generated-uuid-token")
                .user(savedUser)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(verificationService.createVerificationToken(any(User.class))).thenReturn(mockToken);

        authService.register(request);

        verify(verificationService, times(1)).createVerificationToken(any(User.class));
    }

    @Test
    void shouldSendVerificationEmail() {
        // Registering a user successfully sends the verification email
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Test");
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");
        request.setRole(Role.USER);

        when(userRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@123")).thenReturn("hashed");

        UUID userUuid = UUID.randomUUID();
        User savedUser = User.builder()
                .id(userUuid)
                .firstName("Test")
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        VerificationToken mockToken = VerificationToken.builder()
                .token("generated-uuid-token")
                .user(savedUser)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(verificationService.createVerificationToken(any(User.class))).thenReturn(mockToken);

        authService.register(request);

        verify(emailService, times(1)).sendVerificationEmail(eq("test@mail.com"), eq("Test"), anyString());
    }

    @Test
    void shouldBlockLoginBeforeVerification() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
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

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(true) // Verified!
                .enabled(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(UUID.class))).thenReturn("refresh-token");

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

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .firstName("Test")
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

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .firstName("Test")
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

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .firstName("Test")
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

    @Test
    void shouldThrowTooManyRequestsWhenLoginRateLimitExceeded() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ratelimit@mail.com");
        request.setPassword("Test@123");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("rate:limit:login:ratelimit@mail.com")).thenReturn("5");

        assertThrows(TooManyRequestsException.class, () -> authService.login(request));
    }

    @Test
    void shouldIncrementRateLimitCounterAndSetExpiryOnFirstLoginAttempt() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        UUID userUuid = UUID.randomUUID();
        User user = User.builder()
                .id(userUuid)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(UUID.class))).thenReturn("refresh-token");

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("rate:limit:login:test@mail.com")).thenReturn(null);
        when(valueOps.increment("rate:limit:login:test@mail.com")).thenReturn(1L);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        verify(valueOps).increment("rate:limit:login:test@mail.com");
        verify(redisTemplate).expire("rate:limit:login:test@mail.com", 60, java.util.concurrent.TimeUnit.SECONDS);
        verify(redisTemplate).delete("rate:limit:login:test@mail.com");
    }
}