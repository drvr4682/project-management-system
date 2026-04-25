package com.pms.common.fallback;

import com.pms.common.client.AuthFeignClient;
import com.pms.common.dto.UserExistsResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Default fallback for AuthFeignClient.
 *
 * Consuming services can use this directly by registering it as a @Component,
 * or override it with their own fallback by implementing AuthFeignClient.
 *
 * This fallback returns a safe "not found" response rather than throwing,
 * so that the caller can decide how to handle unavailability.
 * Services that must fail-fast should override this.
 */
@Slf4j
public class AuthFeignFallback implements AuthFeignClient {

    @Override
    public UserExistsResponse checkUser(String email) {
        log.error("AUTH SERVICE UNAVAILABLE — fallback triggered for email: {}", email);
        // Return a safe default — caller must check response.isExists()
        return UserExistsResponse.notFound(email);
    }
}