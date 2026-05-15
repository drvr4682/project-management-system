package com.pms.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import com.pms.apigateway.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http
    ) {

        return http

                // Disable default security mechanisms
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                
                // Stateless API Gateway
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance()
                )

                // Route authorization
                .authorizeExchange(exchange -> exchange

                        // Public endpoints
                        .pathMatchers(
                                "/health",
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/health"
                        ).permitAll()

                        // Allow browser preflight requests
                        .pathMatchers(HttpMethod.OPTIONS)
                        .permitAll()

                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )

                // Register custom JWT filter
                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )
                .build();
    }
}