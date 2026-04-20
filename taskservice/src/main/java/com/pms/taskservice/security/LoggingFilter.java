package com.pms.taskservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String correlationId = request.getHeader(CORRELATION_ID);

            if (correlationId != null) {
                MDC.put(CORRELATION_ID, correlationId);
            }

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}