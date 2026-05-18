package com.pms.projectservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.projectservice.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayValidationFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private final ObjectMapper objectMapper;

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String incomingSecret = request.getHeader(GATEWAY_SECRET_HEADER);

        if (incomingSecret == null || !incomingSecret.equals(gatewaySecret)) {

            log.warn(
                    "Blocked direct service access | Path: {}",
                    path
            );

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Direct service access forbidden")
                    .timestamp(System.currentTimeMillis())
                    .path(path)
                    .build();

            response.getWriter().write(
                    objectMapper.writeValueAsString(error)
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/health")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }
}