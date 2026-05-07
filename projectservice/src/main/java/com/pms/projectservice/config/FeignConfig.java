package com.pms.projectservice.config;

import feign.RequestInterceptor;

import lombok.extern.slf4j.Slf4j;

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

            // Forward internal secret for service-to-service auth
            template.header("X-Internal-Secret", internalSecret);

            // Forward JWT so the downstream service can authenticate the caller
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() != null) {
                template.header("Authorization", "Bearer " + auth.getCredentials());
            }
        };
    }
}