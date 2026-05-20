package com.pms.apigateway.exception;

import com.pms.common.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SocketTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(
            SocketTimeoutException ex, HttpServletRequest request) {

        log.error("Gateway timeout occurred for path {}: {}",
                request.getRequestURI(), ex.getMessage());

        return new ResponseEntity<>(
                buildError(HttpStatus.GATEWAY_TIMEOUT, "Downstream service timeout", request),
                HttpStatus.GATEWAY_TIMEOUT
        );
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectionException(
            ConnectException ex, HttpServletRequest request) {

        log.error("Downstream service unavailable for path {}: {}",
                request.getRequestURI(), ex.getMessage());

        return new ResponseEntity<>(
                buildError(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service unavailable", request),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwtException(
            InvalidJwtException ex, HttpServletRequest request) {

        log.warn("Invalid JWT for path {}: {}", request.getRequestURI(), ex.getMessage());

        return new ResponseEntity<>(
                buildError(HttpStatus.UNAUTHORIZED, "Invalid JWT: " + ex.getMessage(), request),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled gateway exception for path {}: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return new ResponseEntity<>(
                buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", request),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ErrorResponse buildError(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .path(request.getRequestURI())
                .build();
    }
}
