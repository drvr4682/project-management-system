package com.pms.authservice.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only RBAC test endpoints.
 * @Profile("dev") ensures these are NEVER registered in production.
 */
@Profile({"dev", "test"})
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "ADMIN access granted";
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public String managerAccess() {
        return "MANAGER access granted";
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public String userAccess() {
        return "USER access granted";
    }
}