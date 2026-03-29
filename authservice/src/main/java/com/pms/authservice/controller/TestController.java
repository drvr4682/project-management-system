package com.pms.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/v1/test/secure")
    public String securedEndpoint() {
        return "Access granted: Secure endpoint";
    }
}