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

            // Internal secret
            template.header("X-Internal-Secret", internalSecret);

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.getCredentials() instanceof String token) {

                template.header(
                        "Authorization",
                        "Bearer " + token
                );
            }
        };
    }
}