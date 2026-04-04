package com.pms.projectservice.client;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthFeignFallback implements AuthFeignClient {
    
    @Override
    public void checkUser(String email) {
        log.error("Auth service is DOWN or unreachable for email: {}", email);

        throw new IllegalArgumentException("Auth service unavailable");
    }
}