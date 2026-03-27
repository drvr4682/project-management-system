package com.pms.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/auth/health")
    public String health() {
        return "Auth Service is running";
    }
}