package com.pms.authservice.controller;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RefreshTokenRequest;
import com.pms.authservice.dto.RefreshTokenResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.service.AuthService;
import com.pms.authservice.service.RefreshTokenService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    // ------------------------------------------------------------------
    // POST /api/v1/auth/register
    // ------------------------------------------------------------------
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/login
    // Response now includes both accessToken (token) and refreshToken
    // ------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/refresh
    // No Authorization header needed — caller sends the refresh token in body.
    // Returns a new access token + a new refresh token (rotation).
    // ------------------------------------------------------------------
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(refreshTokenService.rotate(request));
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/logout
    // Requires a valid JWT (authenticated). Revokes all refresh tokens
    // for the calling user. The access token itself is short-lived and
    // will expire naturally — no server-side access-token blocklist needed.
    // ------------------------------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal String userEmail) {

        authService.logout(userEmail);
        return ResponseEntity.noContent().build();
    }
}