package com.pms.projectservice.util;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatewayHeaderUtil {

    private final HttpServletRequest request;

    public String getAuthenticatedUser() {

        return request.getHeader(
                "X-Authenticated-User"
        );
    }

    public String getAuthenticatedRole() {

        return request.getHeader(
                "X-Authenticated-Role"
        );
    }

    public String getCorrelationId() {

        return request.getHeader(
                "X-Correlation-Id"
        );
    }
}