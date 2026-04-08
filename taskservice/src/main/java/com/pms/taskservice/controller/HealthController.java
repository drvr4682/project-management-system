package com.pms.taskservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/v1/tasks/health")
    public String health() {
        return "Task Service is running";
    }
}