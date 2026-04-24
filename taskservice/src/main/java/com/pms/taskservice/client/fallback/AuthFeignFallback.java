package com.pms.taskservice.client.fallback;

import com.pms.taskservice.client.AuthFeignClient;
import com.pms.taskservice.dto.UserExistsResponse;
import com.pms.taskservice.exception.ServiceUnavailableException;

import org.springframework.stereotype.Component;

@Component
public class AuthFeignFallback implements AuthFeignClient {

    @Override
    public UserExistsResponse checkUser(String email) {
        throw new ServiceUnavailableException("Auth service unavailable");
    }
}