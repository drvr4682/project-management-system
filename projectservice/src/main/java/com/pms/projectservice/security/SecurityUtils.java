package com.pms.projectservice.security;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final HttpServletRequest request;

    public String getCurrentUser() {

        String gatewayUser = request.getHeader("X-Authenticated-User");

        if (gatewayUser != null && !gatewayUser.isBlank()) {
            return gatewayUser;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

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
            if ("anonymousUser".equals(principalString)) {
                return null;
            }
            return principalString;
        }

        return null;
    }

    public String getCurrentRole() {
        return request.getHeader("X-Authenticated-Role");
    }

    public String getCorrelationId() {
        return request.getHeader("X-Correlation-Id");
    }
}