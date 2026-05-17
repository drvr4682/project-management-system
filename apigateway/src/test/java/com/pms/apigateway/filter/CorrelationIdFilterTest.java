package com.pms.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenNoneProvided() throws Exception {

        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain            = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String correlationId = response.getHeader("X-Correlation-Id");

        assertNotNull(correlationId, "CorrelationId should be auto-generated");
        assertFalse(correlationId.isBlank(), "CorrelationId should not be blank");
    }

    @Test
    void shouldPreserveExistingCorrelationId() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "existing-correlation-id");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain            = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(
                "existing-correlation-id",
                response.getHeader("X-Correlation-Id"),
                "Existing correlationId should be preserved"
        );
    }

    // FIX: Added test to verify correlation ID is injected into the downstream request
    @Test
    void shouldInjectCorrelationIdIntoDownstreamRequest() throws Exception {

        MockHttpServletRequest request   = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Use a real FilterChain that captures the wrapped request
        jakarta.servlet.http.HttpServletRequest[] capturedRequest =
                new jakarta.servlet.http.HttpServletRequest[1];

        MockFilterChain chain = new MockFilterChain(
                new jakarta.servlet.http.HttpServlet() {},
                (req, res, fc) -> {
                    capturedRequest[0] = (jakarta.servlet.http.HttpServletRequest) req;
                    fc.doFilter(req, res);
                }
        );

        filter.doFilter(request, response, chain);

        assertNotNull(capturedRequest[0]);
        assertNotNull(
                capturedRequest[0].getHeader("X-Correlation-Id"),
                "Downstream request should have X-Correlation-Id header"
        );
    }
}