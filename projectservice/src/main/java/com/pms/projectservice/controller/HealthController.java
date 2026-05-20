package com.pms.projectservice.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/projects/health")
    public ResponseEntity<Map<String, String>> health() {
                return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "service", "Project Service"
                )
        );
    }
}