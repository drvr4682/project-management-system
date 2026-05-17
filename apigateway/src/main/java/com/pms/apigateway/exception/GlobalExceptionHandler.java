package com.pms.apigateway.exception;

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
            SocketTimeoutException ex,
            HttpServletRequest request
    ) {

        log.error(
                "Gateway timeout occurred for path {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ErrorResponse error =
                ErrorResponse.builder()
                        .status(HttpStatus.GATEWAY_TIMEOUT.value())
                        .message("Downstream service timeout")
                        .timestamp(System.currentTimeMillis())
                        .path(request.getRequestURI())
                        .build();

        return new ResponseEntity<>(
                error,
                HttpStatus.GATEWAY_TIMEOUT
        );
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectionException(
            ConnectException ex,
            HttpServletRequest request
    ) {

        log.error(
                "Downstream service unavailable for path {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ErrorResponse error =
                ErrorResponse.builder()
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .message("Downstream service unavailable")
                        .timestamp(System.currentTimeMillis())
                        .path(request.getRequestURI())
                        .build();

        return new ResponseEntity<>(
                error,
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwtException(
            InvalidJwtException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Invalid JWT for path {}: {}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ErrorResponse error =
                ErrorResponse.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message("Invalid JWT: " + ex.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .path(request.getRequestURI())
                        .build();

        return new ResponseEntity<>(
                error,
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error(
                "Unhandled gateway exception for path {}: {}",
                request.getRequestURI(),
                ex.getMessage(),
                ex
        );

        ErrorResponse error =
                ErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("Something went wrong")
                        .timestamp(System.currentTimeMillis())
                        .path(request.getRequestURI())
                        .build();

        return new ResponseEntity<>(
                error,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}