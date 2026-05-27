package com.pms.projectservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service", 
    url = "${services.auth.url}"
)
public interface AuthFeignClient {
    
    @GetMapping("/internal/auth/users/{userId}")
    String checkUser(
        @PathVariable("userId") String userId
    );
}