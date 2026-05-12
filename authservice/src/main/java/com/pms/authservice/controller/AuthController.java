package com.pms.authservice.controller;

import com.pms.authservice.dto.RegisterRequest;
import com.pms.authservice.dto.RegisterResponse;
import com.pms.authservice.dto.LoginRequest;
import com.pms.authservice.dto.LoginResponse;
import com.pms.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${internal.secret}")
    private String internalSecret;

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/users/{email}")
    public ResponseEntity<String> checkUser(@PathVariable String email, @RequestHeader("X-Internal-Secret") String internalSecret) {

        if (!this.internalSecret.equals(internalSecret)) {
            return ResponseEntity.status(403).body("Invalid internal secret");
        }
        
        boolean exists = authService.userExists(email);

        if (exists) {
            return ResponseEntity.ok("User exists");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}