package com.pms.projectservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "http://localhost:8081")
public interface AuthFeignClient {
    
    @GetMapping("api/v1/auth/user/{email}")
    public String checkUser(@PathVariable("email") String email);
}