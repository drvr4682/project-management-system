package com.pms.projectservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service", 
    url = "http://localhost:8081",
    fallback = AuthFeignFallback.class
)
public interface AuthFeignClient {
    
    @GetMapping("api/v1/auth/users/{email}")
    Boolean checkUser(@PathVariable String email);
}