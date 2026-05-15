package com.pms.projectservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "auth-service", 
    url = "${services.auth.url}"
)
public interface AuthFeignClient {
    
    @GetMapping("/api/v1/auth/users/{email}")
    String checkUser(
        @PathVariable String email,
        @RequestHeader("X-Internal-Secret") String internalSecret 
    );
}