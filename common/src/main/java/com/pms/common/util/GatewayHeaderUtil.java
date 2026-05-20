package com.pms.common.util;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * Convenience accessor for the gateway-injected HTTP headers.
 *
 * <p>The API Gateway injects these headers after authenticating the caller:
 * <ul>
 *   <li>{@code X-Authenticated-User} — the user's email address</li>
 *   <li>{@code X-Authenticated-Role} — the user's role (e.g. "USER", "ADMIN")</li>
 *   <li>{@code X-Correlation-Id}     — the distributed trace correlation ID</li>
 * </ul>
 *
 * <p>Replaces the identical copies in projectservice and taskservice.
 */
@Component
@RequiredArgsConstructor
public class GatewayHeaderUtil {

    private final HttpServletRequest request;

    /** Returns the authenticated user's email from the gateway header, or {@code null}. */
    public String getAuthenticatedUser() {
        return request.getHeader("X-Authenticated-User");
    }

    /** Returns the authenticated user's role from the gateway header, or {@code null}. */
    public String getAuthenticatedRole() {
        return request.getHeader("X-Authenticated-Role");
    }

    /** Returns the correlation ID from the gateway header, or {@code null}. */
    public String getCorrelationId() {
        return request.getHeader("X-Correlation-Id");
    }
}
