package com.pms.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "auth-service",
        url = "http://localhost:8081",
        fallback = com.pms.taskservice.client.fallback.AuthFeignFallback.class
)
public interface AuthFeignClient {

    @GetMapping("/api/v1/auth/users/{email}")
    String checkUser(@PathVariable String email);
}