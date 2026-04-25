package com.pms.authservice.controller;

import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.service.AuthService;
import com.pms.common.dto.UserExistsResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Internal endpoint — called by Project/Task services via Feign.
     * Returns UserExistsResponse from common so the contract is shared.
     */
    @GetMapping("/users/{email}")
    public ResponseEntity<UserExistsResponse> checkUser(@PathVariable String email) {
        boolean exists = authService.userExists(email);
        if (exists) {
            return ResponseEntity.ok(UserExistsResponse.found(email));
        }
        return ResponseEntity.notFound().build();
    }
}