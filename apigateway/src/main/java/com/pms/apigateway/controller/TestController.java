package com.pms.apigateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/admin")
    public ResponseEntity<String> admin() {
        return ResponseEntity.ok("ADMIN SUCCESS");
    }

    @GetMapping("/user")
    public ResponseEntity<String> user() {
        return ResponseEntity.ok("USER SUCCESS");
    }

    @PostMapping("/public")
    public ResponseEntity<String> publicApi() {
        return ResponseEntity.ok("PUBLIC SUCCESS");
    }
}