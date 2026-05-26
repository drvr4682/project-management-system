package com.pms.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.dto.ForgotPasswordRequest;
import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.ResetPasswordRequest;
import com.pms.authservice.entity.PasswordResetToken;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.repository.PasswordResetTokenRepository;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.service.email.EmailService;
import com.pms.authservice.service.password.PasswordResetService;
import com.pms.authservice.service.password.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Autowired
    private PasswordResetService passwordResetService;

    private User verifiedUser;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        if (passwordResetService instanceof PasswordResetServiceImpl) {
            ((PasswordResetServiceImpl) passwordResetService).clearCooldowns();
        }

        // Create a verified and enabled user to test reset password flow
        verifiedUser = User.builder()
                .name("John Reset")
                .email("john.reset@test.com")
                .password("$2a$10$nCoFshq5wO.V.306sE198.g4z4R083lZcQW3d0G6D9w6c.yC2.K2C") // encrypted "Password123!"
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build();
        verifiedUser = userRepository.saveAndFlush(verifiedUser);
    }

    @Test
    void shouldPerformCompletePasswordResetHardenedFlow() throws Exception {
        // ---------------------------------------------------------
        // A. Forgot Password & Assert Token Generation
        // G. Verify Email was Sent
        // ---------------------------------------------------------
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("john.reset@test.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the account exists, a password reset link has been sent."));

        // Verify token is in DB and active
        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserId(verifiedUser.getId());
        assertThat(tokens).isNotEmpty();
        PasswordResetToken token = tokens.get(0);
        assertThat(token.getToken()).isNotNull();
        assertThat(token.isUsed()).isFalse();

        // Verify that reset email was sent via EmailService
        verify(emailService).sendPasswordResetEmail(eq("john.reset@test.com"), eq("John Reset"), anyString());

        // ---------------------------------------------------------
        // H. Cooldown respect (Anti-enumeration does not send email, but returns 200 success)
        // ---------------------------------------------------------
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the account exists, a password reset link has been sent."));

        // Verify that the list size is still 1 (meaning no new token was created during cooldown)
        List<PasswordResetToken> tokensAfterCooldown = passwordResetTokenRepository.findByUserId(verifiedUser.getId());
        assertThat(tokensAfterCooldown.size()).isEqualTo(1);

        // ---------------------------------------------------------
        // B. Expired Token Rejection
        // ---------------------------------------------------------
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token("expired-uuid-token")
                .user(verifiedUser)
                .used(false)
                .expiryDate(LocalDateTime.now().minusMinutes(1))
                .build();
        passwordResetTokenRepository.saveAndFlush(expiredToken);

        ResetPasswordRequest resetReqExpired = new ResetPasswordRequest();
        resetReqExpired.setToken(expiredToken.getToken());
        resetReqExpired.setNewPassword("NewSecurePass123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReqExpired)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password reset link has expired"));

        // ---------------------------------------------------------
        // E. Invalid Token Rejection
        // ---------------------------------------------------------
        ResetPasswordRequest resetReqInvalid = new ResetPasswordRequest();
        resetReqInvalid.setToken("invalid-uuid-token");
        resetReqInvalid.setNewPassword("NewSecurePass123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReqInvalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid password reset token"));

        // ---------------------------------------------------------
        // D. Reset Password Successfully
        // ---------------------------------------------------------
        ResetPasswordRequest resetReqSuccess = new ResetPasswordRequest();
        resetReqSuccess.setToken(token.getToken());
        resetReqSuccess.setNewPassword("NewSecurePass123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReqSuccess)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully. You can now log in with your new password."));

        // Assert token is marked as used
        PasswordResetToken tokenAfterReset = passwordResetTokenRepository.findByToken(token.getToken()).orElse(null);
        assertThat(tokenAfterReset).isNotNull();
        assertThat(tokenAfterReset.isUsed()).isTrue();

        // ---------------------------------------------------------
        // C. Used Token Replay Rejection
        // ---------------------------------------------------------
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReqSuccess)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This password reset token has already been used"));

        // ---------------------------------------------------------
        // J. Reject Old Password After Reset
        // ---------------------------------------------------------
        LoginRequest loginOldReq = new LoginRequest();
        loginOldReq.setEmail("john.reset@test.com");
        loginOldReq.setPassword("Password123!"); // Old password

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginOldReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // ---------------------------------------------------------
        // I. Allow Login With New Password
        // ---------------------------------------------------------
        LoginRequest loginNewReq = new LoginRequest();
        loginNewReq.setEmail("john.reset@test.com");
        loginNewReq.setPassword("NewSecurePass123!"); // New password

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginNewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldInvalidateOlderUnusedTokensOnForgotPassword() throws Exception {
        // Create first reset request
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("john.reset@test.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk());

        List<PasswordResetToken> tokens1 = passwordResetTokenRepository.findByUserId(verifiedUser.getId());
        assertThat(tokens1).isNotEmpty();
        PasswordResetToken token1 = tokens1.get(0);
        assertThat(token1.isUsed()).isFalse();

        // Create direct Bob user to test bulk invalidation in database isolation
        User anotherUser = User.builder()
                .name("Bob Invalidator")
                .email("bob.invalidate@test.com")
                .password("hash")
                .role(Role.USER)
                .enabled(true)
                .emailVerified(true)
                .build();
        anotherUser = userRepository.saveAndFlush(anotherUser);

        // First forgot request Bob
        ForgotPasswordRequest forgotBob = new ForgotPasswordRequest();
        forgotBob.setEmail("bob.invalidate@test.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotBob)))
                .andExpect(status().isOk());

        List<PasswordResetToken> bobTokens1 = passwordResetTokenRepository.findByUserId(anotherUser.getId());
        assertThat(bobTokens1).isNotEmpty();
        PasswordResetToken bobToken1 = bobTokens1.get(0);
        assertThat(bobToken1.isUsed()).isFalse();

        // Create another unused token directly in the database for Bob
        PasswordResetToken directToken = PasswordResetToken.builder()
                .token("bob-active-token-direct")
                .user(anotherUser)
                .used(false)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        passwordResetTokenRepository.saveAndFlush(directToken);

        // Verify Bob has two unused tokens
        List<PasswordResetToken> bobTokensBefore = passwordResetTokenRepository.findByUserId(anotherUser.getId());
        long activeCount = bobTokensBefore.stream().filter(t -> !t.isUsed()).count();
        assertThat(activeCount).isEqualTo(2);

        // Trigger invalidation
        passwordResetTokenRepository.invalidateUnusedTokensByUserId(anotherUser.getId());

        // Assert all previous tokens are now marked used = true
        List<PasswordResetToken> bobTokensAfter = passwordResetTokenRepository.findByUserId(anotherUser.getId());
        boolean hasActive = bobTokensAfter.stream().anyMatch(t -> !t.isUsed());
        assertThat(hasActive).isFalse(); // All are invalidated!
    }

    @Test
    void shouldReturnGenericSuccessResponseEvenIfUserDoesNotExist() throws Exception {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest();
        forgotReq.setEmail("nonexistent.user@test.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the account exists, a password reset link has been sent."));

        // Verify that EmailService was NEVER called
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }
}
