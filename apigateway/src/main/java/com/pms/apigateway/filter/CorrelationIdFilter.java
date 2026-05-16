package com.pms.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime =
                System.currentTimeMillis();

        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_HEADER, correlationId);

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        MutableHttpServletRequest mutableRequest =
                new MutableHttpServletRequest(request);

        mutableRequest.putHeader(CORRELATION_ID_HEADER, correlationId);

        log.info(
                "Incoming Request | Method: {} | URI: {} | CorrelationId: {}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId
        );

        try {

            filterChain.doFilter(mutableRequest, response);

        } finally {

            long duration = System.currentTimeMillis() - startTime;

            log.info(
                    "Completed Response | Status: {} | Duration: {} ms | CorrelationId: {}",
                    response.getStatus(),
                    duration,
                    correlationId
            );

            MDC.clear();
        }
    }
}