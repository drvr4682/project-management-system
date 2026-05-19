package com.pms.taskservice.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Configuration
public class FeignConfig {

    @Value("${internal.secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor requestInterceptor() {

        return template -> {

            // Always send internal secret for service-to-service auth
            template.header("X-Internal-Secret", internalSecret);

            // Propagate correlation ID
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-Id", correlationId);
            }

            // Propagate the JWT token from the current security context
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.getCredentials() instanceof String token) {
                template.header("Authorization", "Bearer " + token);
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
