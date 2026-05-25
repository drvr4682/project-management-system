package com.pms.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.entity.Role;
import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;
import com.pms.authservice.repository.UserRepository;
import com.pms.authservice.repository.VerificationTokenRepository;
import com.pms.authservice.service.email.EmailService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
class EmailVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @BeforeEach
    void cleanUp() {
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldPerformCompleteEmailVerificationHardenedFlow() throws Exception {
        // ---------------------------------------------------------
        // A. Register User & Assert Token Generation
        // ---------------------------------------------------------
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Jane Hardened");
        registerReq.setEmail("jane.hardened@test.com");
        registerReq.setPassword("Password123!");
        registerReq.setRole(Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane.hardened@test.com"));

        // Verify user is in DB but disabled and unverified
        User user = userRepository.findByEmail("jane.hardened@test.com").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isEmailVerified()).isFalse();

        // Verify verification token is generated
        List<VerificationToken> tokens = verificationTokenRepository.findByUserId(user.getId());
        assertThat(tokens).isNotEmpty();
        VerificationToken token = tokens.get(0);
        assertThat(token.getToken()).isNotNull();
        assertThat(token.isUsed()).isFalse();

        // Verify that verification email was sent
        verify(emailService).sendVerificationEmail(eq("jane.hardened@test.com"), eq("Jane Hardened"), anyString());

        // ---------------------------------------------------------
        // B. Login Blocked Before Verification
        // ---------------------------------------------------------
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("jane.hardened@test.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        // ---------------------------------------------------------
        // C. Verification Success
        // ---------------------------------------------------------
        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully. You can now log in."));

        // Assert user is now enabled and verified
        user = userRepository.findByEmail("jane.hardened@test.com").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();

        // Assert token is marked as used
        VerificationToken verifiedToken = verificationTokenRepository.findByToken(token.getToken()).orElse(null);
        assertThat(verifiedToken).isNotNull();
        assertThat(verifiedToken.isUsed()).isTrue();

        // ---------------------------------------------------------
        // D. Expired Token Rejection
        // ---------------------------------------------------------
        VerificationToken expiredToken = VerificationToken.builder()
                .token("expired-uuid-token")
                .user(user)
                .used(false)
                .expiryDate(LocalDateTime.now().minusHours(1))
                .build();
        verificationTokenRepository.save(expiredToken);

        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .param("token", expiredToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification link has expired"));

        // ---------------------------------------------------------
        // E. Used Token Replay Rejection
        // ---------------------------------------------------------
        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .param("token", token.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This verification link has already been used"));

        // ---------------------------------------------------------
        // H. Login Success After Verification
        // ---------------------------------------------------------
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldResendVerificationAndInvalidateOldTokensWithCooldown() throws Exception {
        // Register user first
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Resend Guy");
        registerReq.setEmail("resend.guy@test.com");
        registerReq.setPassword("Password123!");
        registerReq.setRole(Role.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("resend.guy@test.com").orElse(null);
        assertThat(user).isNotNull();

        List<VerificationToken> firstTokens = verificationTokenRepository.findByUserId(user.getId());
        assertThat(firstTokens).isNotEmpty();
        VerificationToken firstToken = firstTokens.get(0);
        assertThat(firstToken.isUsed()).isFalse();

        // ---------------------------------------------------------
        // F. Resend Verification Flow (Success)
        // ---------------------------------------------------------
        ResendVerificationRequest resendReq = new ResendVerificationRequest();
        resendReq.setEmail("resend.guy@test.com");

        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an unverified account exists for that email, a new verification link has been sent."));

        // ---------------------------------------------------------
        // G. New Token Invalidates Old Token (Audit Retention Check)
        // ---------------------------------------------------------
        // Old token must be invalidated (marked used = true)
        VerificationToken oldTokenChecked = verificationTokenRepository.findByToken(firstToken.getToken()).orElse(null);
        assertThat(oldTokenChecked).isNotNull();
        assertThat(oldTokenChecked.isUsed()).isTrue(); // Invalidated!

        // New active token must exist
        List<VerificationToken> secondTokens = verificationTokenRepository.findByUserId(user.getId());
        // Since we are preserving history, we must have at least 2 tokens in DB for this user
        assertThat(secondTokens.size()).isGreaterThanOrEqualTo(2);

        // Find the newly generated active token
        VerificationToken activeToken = secondTokens.stream()
                .filter(t -> !t.getToken().equals(firstToken.getToken()))
                .findFirst().orElse(null);
        assertThat(activeToken).isNotNull();
        assertThat(activeToken.isUsed()).isFalse();

        // Trying to verify using the invalidated old token must fail
        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .param("token", firstToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This verification link has already been used"));

        // ---------------------------------------------------------
        // Cooldown Rejection (Repeated request within short interval)
        // ---------------------------------------------------------
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .header("X-Gateway-Secret", gatewaySecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please wait 60 seconds before requesting another verification email."));
    }
}
