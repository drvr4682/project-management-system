package com.pms.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // AUTH SERVICE
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://localhost:8081"))

                // PROJECT SERVICE
                .route("project-service", r -> r
                        .path("/api/v1/projects/**")
                        .uri("http://localhost:8082"))

                // TASK SERVICE
                .route("task-service", r -> r
                        .path("/api/v1/tasks/**")
                        .uri("http://localhost:8083"))

                .build();
    }
}