package com.pms.apigateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.exception.ErrorResponse;
import com.pms.apigateway.filter.CorrelationIdFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    public static final String USER_EMAIL_HEADER =
            "X-User-Email";

    public static final String USER_ROLE_HEADER =
            "X-User-Role";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        // Public endpoints
        if (path.equals("/health")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/health")) {

            return chain.filter(exchange);
        }

        String authHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            return unauthorizedResponse(
                    exchange,
                    "Missing or invalid Authorization header"
            );
        }

        String token =
                authHeader.substring(7);

        try {

            if (!jwtUtil.validateToken(token)) {

                return unauthorizedResponse(
                        exchange,
                        "Invalid JWT"
                );
            }

            String email =
                    jwtUtil.extractUsername(token);

            String role =
                    jwtUtil.extractRole(token);

            String correlationId =
                    exchange.getRequest()
                            .getHeaders()
                            .getFirst(
                                    CorrelationIdFilter.CORRELATION_ID_HEADER
                            );

            // Add trusted headers for downstream services
            ServerHttpRequest mutatedRequest =
                    exchange.getRequest()
                            .mutate()

                            .header(
                                    USER_EMAIL_HEADER,
                                    email
                            )

                            .header(
                                    USER_ROLE_HEADER,
                                    role
                            )

                            .header(
                                    CorrelationIdFilter.CORRELATION_ID_HEADER,
                                    correlationId
                            )

                            .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                    );

            log.info("Authenticated user: {} with role: {}", email, role);

            return chain.filter(
                            exchange.mutate()
                                    .request(mutatedRequest)
                                    .build()
                    )
                    .contextWrite(
                            ReactiveSecurityContextHolder.withAuthentication(
                                    authentication
                            )
                    );

        } catch (Exception e) {

            log.error("JWT validation failed: {}", e.getMessage());

            return unauthorizedResponse(
                    exchange,
                    "Invalid JWT"
            );
        }
    }

    private Mono<Void> unauthorizedResponse(
            ServerWebExchange exchange,
            String message
    ) {

        try {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            exchange.getResponse()
                    .getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);

            ErrorResponse error =
                    ErrorResponse.builder()
                            .status(401)
                            .message(message)
                            .timestamp(System.currentTimeMillis())
                            .path(
                                    exchange.getRequest()
                                            .getPath()
                                            .value()
                            )
                            .build();

            byte[] bytes =
                    objectMapper.writeValueAsString(error)
                            .getBytes(StandardCharsets.UTF_8);

            return exchange.getResponse()
                    .writeWith(
                            Mono.just(
                                    exchange.getResponse()
                                            .bufferFactory()
                                            .wrap(bytes)
                            )
                    );

        } catch (Exception e) {

            return Mono.error(e);
        }
    }
}