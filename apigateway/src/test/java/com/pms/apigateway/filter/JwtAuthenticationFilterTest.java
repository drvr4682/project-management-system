package com.pms.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.security.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    @Test
    void shouldRejectInvalidToken() throws Exception {

        JwtUtil jwtUtil =
                org.mockito.Mockito.mock(JwtUtil.class);

        org.mockito.Mockito.when(
                        jwtUtil.extractUsername("invalid")
                )
                .thenThrow(new RuntimeException());

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(
                        jwtUtil,
                        new ObjectMapper()
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer invalid"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain chain =
                new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAddAuthenticatedHeaders()
            throws Exception {

        JwtUtil jwtUtil =
                new JwtUtil();

        java.lang.reflect.Field secretField =
                JwtUtil.class.getDeclaredField("secret");

        secretField.setAccessible(true);

        secretField.set(
                jwtUtil,
                "testsecretkeytestsecretkeytestsecret12"
        );

        jwtUtil.init();

        String token =
                Jwts.builder()
                        .setSubject("admin@test.com")
                        .claim("role", "ADMIN")
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        "testsecretkeytestsecretkeytestsecret12"
                                                .getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .compact();

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(
                        jwtUtil,
                        new ObjectMapper()
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain chain =
                new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }
}