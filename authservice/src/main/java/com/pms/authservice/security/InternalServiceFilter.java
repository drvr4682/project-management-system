package com.pms.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class InternalServiceFilter extends OncePerRequestFilter {

    private final String internalSecret;
    private final ObjectMapper objectMapper;

    public InternalServiceFilter(String internalSecret, ObjectMapper objectMapper) {
        this.internalSecret = internalSecret;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.contains("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestSecret = request.getHeader("X-Internal-Secret");

        if (requestSecret == null || !requestSecret.equals(internalSecret)) {

            log.warn("Unauthorized internal request: {}", path);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");

            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .message("Invalid internal service secret")
                    .timestamp(System.currentTimeMillis())
                    .path(path)
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        log.info("Authorized internal request: {}", path);

        filterChain.doFilter(request, response);
    }
}