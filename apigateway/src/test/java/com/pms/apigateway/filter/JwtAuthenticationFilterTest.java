package com.pms.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.security.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET =
            "testsecretkeytestsecretkeytestsecret12";

    @Test
    void shouldRejectInvalidToken() throws Exception {

        JwtUtil jwtUtil = org.mockito.Mockito.mock(JwtUtil.class);

        org.mockito.Mockito
                .when(jwtUtil.validateToken("invalid"))
                .thenReturn(false);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain            = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldPassThroughWhenNoAuthorizationHeader() throws Exception {

        JwtUtil jwtUtil = org.mockito.Mockito.mock(JwtUtil.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());

        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain            = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAddAuthenticatedHeaders() throws Exception {

        JwtUtil jwtUtil = new JwtUtil();

        java.lang.reflect.Field secretField =
                JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, TEST_SECRET);
        jwtUtil.init();

        String token =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        TEST_SECRET.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<HttpServletRequest> capturedRequest = new AtomicReference<>();

        Filter capturingFilter = (ServletRequest req, ServletResponse res, FilterChain fc) -> {
            capturedRequest.set((HttpServletRequest) req);
            // Do NOT call fc.doFilter — this is the last filter; no servlet to invoke
        };

        MockFilterChain chain = new MockFilterChain(
                new jakarta.servlet.http.HttpServlet() {},
                capturingFilter
        );

        filter.doFilter(request, response, chain);

        // Status stays at 200 because we never invoke an unimplemented servlet
        assertEquals(200, response.getStatus());

        // Verify authenticated headers were injected into the downstream request
        assertNotNull(capturedRequest.get(), "Downstream request should have been captured");
        assertEquals(
                "admin@test.com",
                capturedRequest.get().getHeader("X-Authenticated-User"),
                "X-Authenticated-User header should be set"
        );
        assertEquals(
                "ADMIN",
                capturedRequest.get().getHeader("X-Authenticated-Role"),
                "X-Authenticated-Role header should be set"
        );
    }

    @Test
    void shouldSkipFilterForPublicEndpoints() throws Exception {

        JwtUtil jwtUtil = org.mockito.Mockito.mock(JwtUtil.class);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtUtil, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain            = new MockFilterChain();

        filter.doFilter(request, response, chain);

        org.mockito.Mockito.verify(jwtUtil, org.mockito.Mockito.never())
                .validateToken(org.mockito.Mockito.anyString());

        assertEquals(200, response.getStatus());
    }
}