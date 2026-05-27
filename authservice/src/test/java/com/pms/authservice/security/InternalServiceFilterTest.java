package com.pms.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalServiceFilterTest {

    private InternalServiceFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceFilter("testinternalkey", new ObjectMapper());
    }

    @Test
    void shouldAllowValidInternalRequest() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/auth/users/test@mail.com");
        request.addHeader("X-Internal-Secret", "testinternalkey");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectInvalidInternalRequest() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/auth/users/test@mail.com");
        request.addHeader("X-Internal-Secret", "wrong-secret");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldRejectRequestWithMissingSecret() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/internal/auth/users/test@mail.com");
        // No X-Internal-Secret header

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldPassThroughNonInternalRequest() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/health");
        // No secret needed — not an /internal/ path

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }
}