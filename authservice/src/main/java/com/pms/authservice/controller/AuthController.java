package com.pms.authservice.controller;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.dto.ResendVerificationRequest;
import com.pms.authservice.service.AuthService;

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
}