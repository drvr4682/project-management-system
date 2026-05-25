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
import com.pms.authservice.repository.VerificationTokenRepository;
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
    private final VerificationTokenRepository verificationTokenRepository = Mockito.mock(VerificationTokenRepository.class);
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
            userRepository, verificationTokenRepository, passwordEncoder, jwtUtil,
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

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@mail.com", response.getEmail());

        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
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
    void shouldThrowWhenLoginUnverified() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("Test@123");

        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .password("hashed")
                .role(Role.USER)
                .emailVerified(false)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(null);

        assertThrows(EmailVerificationException.class, () -> authService.login(request));
    }

    @Test
    void shouldVerifyEmailSuccessfully() {
        String token = "valid-token";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .emailVerified(false)
                .build();

        VerificationToken vToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(vToken));

        authService.verifyEmail(token);

        assertTrue(user.isEmailVerified());
        assertTrue(vToken.isUsed());
        verify(userRepository, times(1)).save(user);
        verify(verificationTokenRepository, times(1)).save(vToken);
    }

    @Test
    void shouldThrowWhenVerifyEmailWithExpiredToken() {
        String token = "expired-token";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .emailVerified(false)
                .build();

        VerificationToken vToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(vToken));

        assertThrows(EmailVerificationException.class, () -> authService.verifyEmail(token));
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenVerifyEmailWithAlreadyUsedToken() {
        String token = "used-token";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .emailVerified(false)
                .build();

        VerificationToken vToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(true)
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(vToken));

        assertThrows(EmailVerificationException.class, () -> authService.verifyEmail(token));
        assertFalse(user.isEmailVerified());
        verify(userRepository, never()).save(any());
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

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        authService.resendVerificationEmail(request);

        verify(verificationTokenRepository, times(1)).deleteByUserId(1L);
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendResendVerificationEmail(eq("test@mail.com"), eq("Test"), anyString());
    }

    @Test
    void shouldResendVerificationEmailSilentlyIfAlreadyVerified() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@mail.com");

        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("test@mail.com")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        authService.resendVerificationEmail(request);

        verify(verificationTokenRepository, never()).deleteByUserId(anyLong());
        verify(verificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendResendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectExpiredVerificationToken() {
        String token = "expired-token-uuid";
        User user = User.builder()
                .id(1L)
                .email("test@mail.com")
                .emailVerified(false)
                .enabled(false)
                .build();

        VerificationToken vToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().minusHours(2)) // expiry in past
                .used(false)
                .build();

        when(verificationTokenRepository.findByToken(token)).thenReturn(Optional.of(vToken));

        assertThrows(EmailVerificationException.class, () -> authService.verifyEmail(token));
        assertFalse(user.isEmailVerified());
        assertFalse(user.isEnabled());
        verify(userRepository, never()).save(any());
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
}