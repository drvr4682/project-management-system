package com.pms.apigateway.filter;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    @Test
    void shouldGenerateCorrelationId()
            throws Exception {

        CorrelationIdFilter filter =
                new CorrelationIdFilter();

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain chain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                chain
        );

        String correlationId =
                response.getHeader(
                        "X-Correlation-Id"
                );

        assertNotNull(correlationId);
    }

    @Test
    void shouldPreserveExistingCorrelationId()
            throws Exception {

        CorrelationIdFilter filter =
                new CorrelationIdFilter();

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "X-Correlation-Id",
                "existing-correlation-id"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain chain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                chain
        );

        assertEquals(
                "existing-correlation-id",
                response.getHeader("X-Correlation-Id")
        );
    }
}