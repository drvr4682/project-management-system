package com.pms.projectservice.security;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    private HttpServletRequest request;

    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {

        request =
                Mockito.mock(HttpServletRequest.class);

        securityUtils =
                new SecurityUtils(request);

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnGatewayHeaderUser() {

        Mockito.when(
                request.getHeader(
                        "X-Authenticated-User"
                )
        ).thenReturn("gateway@test.com");

        String user =
                securityUtils.getCurrentUser();

        assertEquals(
                "gateway@test.com",
                user
        );
    }

    @Test
    void shouldFallbackToSecurityContext() {

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "security@test.com",
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        String user =
                securityUtils.getCurrentUser();

        assertEquals(
                "security@test.com",
                user
        );
    }

    @Test
    void shouldReturnRoleFromGatewayHeader() {

        Mockito.when(
                request.getHeader(
                        "X-Authenticated-Role"
                )
        ).thenReturn("ADMIN");

        assertEquals(
                "ADMIN",
                securityUtils.getCurrentRole()
        );
    }
}