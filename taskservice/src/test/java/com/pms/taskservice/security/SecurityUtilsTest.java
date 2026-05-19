package com.pms.taskservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @Test
    void getCurrentUser_fromGatewayHeader_returnsHeaderValue() {
        when(request.getHeader("X-Authenticated-User")).thenReturn("gateway@example.com");
        assertThat(securityUtils.getCurrentUser()).isEqualTo("gateway@example.com");
    }

    @Test
    void getCurrentUser_fromSecurityContext_returnsUsername() {
        when(request.getHeader("X-Authenticated-User")).thenReturn(null);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "ctx@example.com", "token",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        assertThat(securityUtils.getCurrentUser()).isEqualTo("ctx@example.com");
    }

    @Test
    void getCurrentUser_anonymous_returnsNull() {
        when(request.getHeader("X-Authenticated-User")).thenReturn(null);

        AnonymousAuthenticationToken anonAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(anonAuth);
        SecurityContextHolder.setContext(ctx);

        assertThat(securityUtils.getCurrentUser()).isNull();
    }

    @Test
    void getCurrentUser_noAuth_returnsNull() {
        when(request.getHeader("X-Authenticated-User")).thenReturn(null);
        assertThat(securityUtils.getCurrentUser()).isNull();
    }

    @Test
    void getCorrelationId_returnsHeaderValue() {
        when(request.getHeader("X-Correlation-Id")).thenReturn("abc-123");
        assertThat(securityUtils.getCorrelationId()).isEqualTo("abc-123");
    }

    @Test
    void getCurrentRole_returnsHeaderValue() {
        when(request.getHeader("X-Authenticated-Role")).thenReturn("USER");
        assertThat(securityUtils.getCurrentRole()).isEqualTo("USER");
    }
}
