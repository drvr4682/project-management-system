package com.pms.taskservice.client.fallback;

import com.pms.taskservice.client.AuthFeignClient;
import org.springframework.stereotype.Component;

@Component
public class AuthFeignFallback implements AuthFeignClient {

    @Override
    public String checkUser(String email) {
        throw new RuntimeException("Auth service unavailable");
    }
}