package com.pms.taskservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.pms.taskservice.client.fallback.AuthFeignFallback;
import com.pms.taskservice.dto.UserExistsResponse;

@FeignClient(
    name = "auth-service",
    url = "${services.auth.url}",
    fallback = AuthFeignFallback.class
)
public interface AuthFeignClient {

    @GetMapping("/api/v1/auth/users/{email}")
    UserExistsResponse checkUser(@PathVariable String email);
}