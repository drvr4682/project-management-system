package com.pms.taskservice.service;

import com.pms.taskservice.client.AuthFeignClient;
import com.pms.taskservice.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthValidationComponent {

    private final AuthFeignClient authFeignClient;

    @CircuitBreaker(name = "authService", fallbackMethod = "validateUserFallback")
    @Retry(name = "authService")
    public String validateUser(String userId) {
        log.info("Calling AuthService to validate user: {}", userId);
        return authFeignClient.checkUser(userId);
    }

    public String validateUserFallback(String userId, Throwable throwable) {
        log.error(
                "AuthService fallback triggered for user: {} | Error: {}",
                userId, throwable.getMessage()
        );

        if (throwable instanceof IllegalArgumentException iae) {
            throw iae;
        }

        throw new ServiceUnavailableException(
                "Auth service unavailable: " + throwable.getMessage()
        );
    }
}
