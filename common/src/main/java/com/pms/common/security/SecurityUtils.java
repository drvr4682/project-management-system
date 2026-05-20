package com.pms.common.security;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Shared utility for reading the authenticated user from either the
 * gateway-injected headers ({@code X-Authenticated-User} /
 * {@code X-Authenticated-Role}) or the Spring {@link SecurityContextHolder}.
 *
 * <p>Gateway header takes precedence because downstream services receive
 * pre-validated identity from the API Gateway.
 *
 * <p>Replaces the identical copies in projectservice and taskservice.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final HttpServletRequest request;

    /**
     * Returns the current user's email/username.
     * Checks the {@code X-Authenticated-User} header first, then the
     * Spring security context. Returns {@code null} if no authenticated
     * user is found.
     */
    public String getCurrentUser() {

        String gatewayUser = request.getHeader("X-Authenticated-User");
        if (gatewayUser != null && !gatewayUser.isBlank()) {
            return gatewayUser;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String principalString) {
            return "anonymousUser".equals(principalString) ? null : principalString;
        }

        return null;
    }

    /**
     * Returns the role from the {@code X-Authenticated-Role} gateway header,
     * or {@code null} if absent.
     */
    public String getCurrentRole() {
        return request.getHeader("X-Authenticated-Role");
    }

    /**
     * Returns the correlation ID from the {@code X-Correlation-Id} header,
     * or {@code null} if absent.
     */
    public String getCorrelationId() {
        return request.getHeader("X-Correlation-Id");
    }
}
