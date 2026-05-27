package com.pms.common.security;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    // getCurrentUser
    @Test
    @DisplayName("Returns gateway header value when X-User-Id is present")
    void getCurrentUser_gatewayHeaderPresent_returnsHeader() {
        when(request.getHeader("X-User-Id")).thenReturn("gateway-uuid-123");
        assertThat(securityUtils.getCurrentUser()).isEqualTo("gateway-uuid-123");
    }

    @Test
    @DisplayName("Falls back to SecurityContext when gateway header is absent")
    void getCurrentUser_noGatewayHeader_fallsBackToSecurityContext() {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        "ctx@test.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        assertThat(securityUtils.getCurrentUser()).isEqualTo("ctx@test.com");
    }

    @Test
    @DisplayName("Returns null when principal is anonymousUser string")
    void getCurrentUser_anonymousUser_returnsNull() {
        when(request.getHeader("X-User-Id")).thenReturn(null);

        AnonymousAuthenticationToken anonAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(anonAuth);
        SecurityContextHolder.setContext(ctx);

        assertThat(securityUtils.getCurrentUser()).isNull();
    }

    @Test
    @DisplayName("Returns null when no authentication exists at all")
    void getCurrentUser_noAuth_returnsNull() {
        when(request.getHeader("X-User-Id")).thenReturn(null);
        assertThat(securityUtils.getCurrentUser()).isNull();
    }

    // getCurrentRole / getCorrelationId
    @Test
    @DisplayName("getCurrentRole returns X-User-Role header value")
    void getCurrentRole_returnsHeader() {
        when(request.getHeader("X-User-Role")).thenReturn("ADMIN");
        assertThat(securityUtils.getCurrentRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("getCorrelationId returns X-Correlation-Id header value")
    void getCorrelationId_returnsHeader() {
        when(request.getHeader("X-Correlation-Id")).thenReturn("abc-123");
        assertThat(securityUtils.getCorrelationId()).isEqualTo("abc-123");
    }
}
