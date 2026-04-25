package com.pms.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Value("${services.auth.url}")
    private String authServiceUrl;

    @Value("${services.project.url}")
    private String projectServiceUrl;

    @Value("${services.task.url}")
    private String taskServiceUrl;

    @Value("${rate.limit.replenish-rate:10}")
    private int replenishRate;

    @Value("${rate.limit.burst-capacity:20}")
    private int burstCapacity;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // Auth Service — no rate limit on auth endpoints
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri(authServiceUrl))

                // Project Service — rate limited
                .route("project-service", r -> r
                        .path("/api/v1/projects/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter());
                                    c.setKeyResolver(ipKeyResolver());
                                })
                        )
                        .uri(projectServiceUrl))

                // Task Service — rate limited
                .route("task-service", r -> r
                        .path("/api/v1/tasks/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter());
                                    c.setKeyResolver(ipKeyResolver());
                                })
                        )
                        .uri(taskServiceUrl))

                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .defaultIfEmpty("unknown");
    }
}