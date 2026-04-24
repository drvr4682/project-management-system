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

    @Value("${rate.limit.replenish-rate:10}")
    private int replenishRate;

    @Value("${rate.limit.burst-capacity:20}")
    private int burstCapacity;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // Auth Service — public, no rate limit on auth endpoints
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://localhost:8081"))

                // Project Service — rate limited
                .route("project-service", r -> r
                        .path("/api/v1/projects/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter());
                                    c.setKeyResolver(ipKeyResolver());
                                })
                        )
                        .uri("http://localhost:8082"))

                // Task Service — rate limited
                .route("task-service", r -> r
                        .path("/api/v1/tasks/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(redisRateLimiter());
                                    c.setKeyResolver(ipKeyResolver());
                                })
                        )
                        .uri("http://localhost:8083"))

                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity);
    }

    // Rate limit by IP address
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .defaultIfEmpty("unknown");
    }
}