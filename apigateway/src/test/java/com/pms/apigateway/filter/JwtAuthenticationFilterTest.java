package com.pms.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.apigateway.security.JwtUtil;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    @Test
    void shouldRejectInvalidToken() throws Exception {

        JwtUtil jwtUtil =
                Mockito.mock(JwtUtil.class);

        Mockito.when(jwtUtil.extractUsername("invalid"))
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
}