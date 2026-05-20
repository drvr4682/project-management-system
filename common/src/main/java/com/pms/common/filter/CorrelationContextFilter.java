package com.pms.common.filter;

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

/**
 * Propagates (or generates) a correlation ID for distributed tracing.
 *
 * <p>Reads the {@code X-Correlation-Id} header; generates a fresh UUID when
 * absent. The ID is stored in {@link MDC} under the key {@code correlationId}
 * and echoed back to the caller via the response header.
 *
 * <p>Replaces the three per-service copies (authservice, projectservice,
 * taskservice). The authservice copy did not generate a UUID — that behaviour
 * is now unified to always ensure a correlation ID exists.
 */
@Slf4j
@Component
public class CorrelationContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        log.info("Incoming Request | Method: {} | URI: {} | CorrelationId: {}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
