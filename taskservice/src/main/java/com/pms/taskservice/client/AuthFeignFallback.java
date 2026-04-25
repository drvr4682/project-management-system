package com.pms.taskservice.client;

import com.pms.common.client.AuthFeignClient;
import com.pms.common.dto.UserExistsResponse;
import com.pms.taskservice.exception.ServiceUnavailableException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
public class AuthFeignFallback implements AuthFeignClient {

    @Override
    public UserExistsResponse checkUser(String email) {
        log.error("Auth service unavailable — fallback triggered for email: {}", email);
        throw new ServiceUnavailableException("Auth service is currently unavailable");
    }
}