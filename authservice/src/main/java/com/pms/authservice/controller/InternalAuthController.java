package com.pms.authservice.controller;

import com.pms.authservice.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final AuthService authService;

    @GetMapping("/users/{email}")
    public ResponseEntity<String> checkUser(@PathVariable String email) {

        boolean exists = authService.userExists(email);

        if (exists) {
            return ResponseEntity.ok("User exists");
        }

        return ResponseEntity.notFound().build();
    }
}