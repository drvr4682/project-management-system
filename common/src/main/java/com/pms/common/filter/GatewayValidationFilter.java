package com.pms.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.exception.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks direct HTTP access to microservices that bypasses the API Gateway.
 *
 * <p>Every request must carry the {@code X-Gateway-Secret} header whose value
 * matches the configured {@code gateway.secret} property. Requests that fail
 * this check receive a 403 JSON response.
 *
 * <p>Replaces the three nearly-identical per-service copies in authservice,
 * projectservice, and taskservice. Callers register it as a Spring bean and
 * supply the gateway secret via constructor injection.
 *
 * <h3>Exempt paths</h3>
 * Sub-classes or the wrapping bean can override {@link #isPublicPath(String)}
 * to add service-specific exemptions. The base implementation exempts:
 * <ul>
 *   <li>{@code /health}</li>
 *   <li>{@code /actuator/health}</li>
 *   <li>{@code /actuator/info}</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayValidationFilter extends OncePerRequestFilter {

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final String gatewaySecret;
    private final ObjectMapper objectMapper;

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
            log.warn("Blocked direct service access | Path: {}", path);

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ErrorResponse error = ErrorResponse.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Direct service access forbidden")
                    .timestamp(System.currentTimeMillis())
                    .path(path)
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns {@code true} for paths that should bypass the gateway-secret
     * check. Override in sub-classes to add service-specific exemptions.
     */
    protected boolean isPublicPath(String path) {
        return path.equals("/health")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }
}
