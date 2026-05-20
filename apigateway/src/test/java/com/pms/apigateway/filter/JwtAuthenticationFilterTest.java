package com.pms.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.security.JwtAuthenticationFilter;
import com.pms.common.security.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET =
            "testsecretkeytestsecretkeytestsecret12";

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 86_400_000L);
        filter = new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Returns 401 when token is invalid")
    void shouldRejectInvalidToken() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        when(mockJwt.validateToken("invalid")).thenReturn(false);

        JwtAuthenticationFilter f = new JwtAuthenticationFilter(mockJwt, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        f.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("Passes through when no Authorization header is present")
    void shouldPassThroughWhenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Authenticates and populates SecurityContext for a valid token")
    void shouldAuthenticateValidToken() throws Exception {
        String token = Jwts.builder()
                .setSubject("admin@test.com")
                .claim("role", "ADMIN")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain         chain    = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin@test.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Skips JWT validation for /health path")
    void shouldSkipFilterForHealthEndpoint() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(mockJwt, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        f.doFilter(request, response, new MockFilterChain());

        verify(mockJwt, never()).validateToken(anyString());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Skips JWT validation for /api/v1/auth/login path")
    void shouldSkipFilterForLoginEndpoint() throws Exception {
        JwtUtil mockJwt = mock(JwtUtil.class);
        JwtAuthenticationFilter f = new JwtAuthenticationFilter(mockJwt, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        f.doFilter(request, response, new MockFilterChain());

        verify(mockJwt, never()).validateToken(anyString());
        assertEquals(200, response.getStatus());
    }
}