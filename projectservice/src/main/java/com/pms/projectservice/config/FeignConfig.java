package com.pms.projectservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.pms.projectservice.security.JwtFilter.currentToken;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            String token = currentToken.get();
            
            if (token != null) {
                template.header("Authorization", "Bearer " + token);
            }
        };
    }
}