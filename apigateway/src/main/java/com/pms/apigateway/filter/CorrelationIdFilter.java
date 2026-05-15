package com.pms.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID =
            "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId =
                request.getHeader(CORRELATION_ID);

        if (correlationId == null
                || correlationId.isBlank()) {

            correlationId =
                    UUID.randomUUID().toString();
        }

        request.setAttribute(
                CORRELATION_ID,
                correlationId
        );

        response.setHeader(
                CORRELATION_ID,
                correlationId
        );

        log.info(
                "Incoming Request | Method: {} | URI: {} | CorrelationId: {}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId
        );

        filterChain.doFilter(request, response);
    }
}