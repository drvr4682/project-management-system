package com.pms.taskservice.config;

import feign.RequestInterceptor;

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

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Value("${internal.secret}")
    private String internalSecret;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {

            // Forward Correlation ID for distributed tracing
            String correlationId = MDC.get(CORRELATION_ID_HEADER);
            if (correlationId != null) {
                template.header(CORRELATION_ID_HEADER, correlationId);
                log.debug("Forwarding Correlation ID: {}", correlationId);
            }

            // Forward internal secret
            template.header("X-Internal-Secret", internalSecret);

            // Forward JWT if available
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() != null) {
                template.header("Authorization", "Bearer " + auth.getCredentials());
            }
        };
    }
}