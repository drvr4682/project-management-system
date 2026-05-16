package com.pms.apigateway.exception;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import org.springframework.http.ResponseEntity;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldHandleTimeoutException() {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/v1/projects");

        ResponseEntity<ErrorResponse> response =
                handler.handleTimeoutException(
                        new SocketTimeoutException("Timeout"),
                        request
                );

        assertEquals(
                504,
                response.getStatusCode().value()
        );

        assertEquals(
                "Downstream service timeout",
                response.getBody().getMessage()
        );
    }

    @Test
    void shouldHandleConnectionException() {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/v1/projects");

        ResponseEntity<ErrorResponse> response =
                handler.handleConnectionException(
                        new ConnectException("Connection failed"),
                        request
                );

        assertEquals(
                503,
                response.getStatusCode().value()
        );

        assertEquals(
                "Downstream service unavailable",
                response.getBody().getMessage()
        );
    }

    @Test
    void shouldHandleGenericException() {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setRequestURI("/api/v1/projects");

        ResponseEntity<ErrorResponse> response =
                handler.handleException(
                        new RuntimeException("Unexpected"),
                        request
                );

        assertEquals(
                500,
                response.getStatusCode().value()
        );

        assertEquals(
                "Something went wrong",
                response.getBody().getMessage()
        );
    }
}