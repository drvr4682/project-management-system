package com.pms.common.client;

import com.pms.common.dto.UserExistsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client interface for Auth Service.
 *
 * Consuming services (Project, Task) use this interface directly.
 * Each consuming service registers its own fallback via @FeignClient
 * overrides or Resilience4j — not hardcoded here.
 *
 * URL is resolved from each service's own application.properties:
 *   services.auth.url=${AUTH_SERVICE_URL:http://localhost:8081}
 */
@FeignClient(
    name = "auth-service",
    url = "${services.auth.url}"
)
public interface AuthFeignClient {

    @GetMapping("/api/v1/auth/users/{email}")
    UserExistsResponse checkUser(@PathVariable String email);
}