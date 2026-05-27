package com.pms.authservice.controller;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.dto.ForgotPasswordRequest;
import com.pms.authservice.dto.ResetPasswordRequest;
import com.pms.authservice.dto.UserSummaryDTO;
import com.pms.authservice.dto.ChangePasswordRequest;
import com.pms.authservice.service.AuthService;
import com.pms.authservice.service.password.PasswordResetService;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    // =========================================================================
    // REGISTER
    // =========================================================================

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    // =========================================================================
    // TOKEN REFRESH
    // =========================================================================

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(authService.refresh(request));
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal String userEmail,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        authService.logout(userEmail, accessToken);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // =========================================================================
    // EMAIL VERIFICATION — verify link click
    // =========================================================================

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @RequestParam("token") String token) {

        authService.verifyEmail(token);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully. You can now log in."));
    }

    // =========================================================================
    // EMAIL VERIFICATION — resend link
    // =========================================================================

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        authService.resendVerificationEmail(request);

        return ResponseEntity.ok(Map.of(
                "message", "If an unverified account exists for that email, "
                           + "a new verification link has been sent."));
    }

    // =========================================================================
    // FORGOT PASSWORD
    // =========================================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.handleForgotPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "If the account exists, a password reset link has been sent."));
    }

    // =========================================================================
    // RESET PASSWORD
    // =========================================================================

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.handleResetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Password has been reset successfully. You can now log in with your new password."));
    }

    // =========================================================================
    // CHANGE PASSWORD (AUTHENTICATED)
    // =========================================================================

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("[Auth] Received change password request for user: {}", userEmail);
        authService.changePassword(userEmail, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    // =========================================================================
    // SEARCH USERS
    // =========================================================================

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDTO>> searchUsers(
            @RequestParam(value = "query", defaultValue = "") String query) {
        log.info("[Auth] Search users with query: {}", query);
        return ResponseEntity.ok(authService.searchUsers(query));
    }
}