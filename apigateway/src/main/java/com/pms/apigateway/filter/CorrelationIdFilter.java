package com.pms.apigateway.filter;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER =
            "X-Correlation-Id";

    @Override
    public Mono<Void> filter(
            org.springframework.web.server.ServerWebExchange exchange,
            org.springframework.cloud.gateway.filter.GatewayFilterChain chain
    ) {

        String correlationId =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(CORRELATION_ID_HEADER);

        // Reuse existing correlation ID if already present
        if (correlationId == null || correlationId.isBlank()) {

            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header(
                                CORRELATION_ID_HEADER,
                                correlationId
                        )
                        .build();

        log.info(
                "Correlation ID: {} | Method: {} | Path: {}",
                correlationId,
                mutatedRequest.getMethod(),
                mutatedRequest.getURI().getPath()
        );

        return chain.filter(
                exchange.mutate()
                        .request(mutatedRequest)
                        .build()
        );
    }

    @Override
    public int getOrder() {

        // Execute very early
        return -1;
    }
}