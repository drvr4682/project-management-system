package com.pms.projectservice.client;

import com.pms.common.client.AuthFeignClient;
import com.pms.common.dto.UserExistsResponse;
import com.pms.projectservice.exception.ServiceUnavailableException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Project service's own fallback for AuthFeignClient.
 * Throws ServiceUnavailableException so the caller gets a proper HTTP 503.
 *
 * Registered as @Primary so Spring uses this over any default fallback.
 */
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