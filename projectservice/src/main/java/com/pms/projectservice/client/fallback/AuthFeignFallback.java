package com.pms.projectservice.client.fallback;

import com.pms.projectservice.client.AuthFeignClient;
import com.pms.projectservice.dto.UserExistsResponse;
import com.pms.projectservice.exception.ServiceUnavailableException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthFeignFallback implements AuthFeignClient {

    @Override
    public UserExistsResponse checkUser(String email) {
        log.error("Auth service is DOWN or unreachable for email: {}", email);
        throw new ServiceUnavailableException("Auth service is down");
    }
}