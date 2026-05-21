package com.pms.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GatewayValidationFilterTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";

    private GatewayValidationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new GatewayValidationFilter(GATEWAY_SECRET, new ObjectMapper());
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Allows request when correct gateway secret header is present")
    void doFilter_correctSecret_proceedsToChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/projects");
        request.addHeader("X-Gateway-Secret", GATEWAY_SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Returns 403 when gateway secret header is missing")
    void doFilter_missingSecret_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/projects");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Returns 403 when gateway secret header has wrong value")
    void doFilter_wrongSecret_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/projects");
        request.addHeader("X-Gateway-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Bypasses check for /health path")
    void doFilter_healthPath_bypassesCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bypasses check for /actuator/health path")
    void doFilter_actuatorHealthPath_bypassesCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
