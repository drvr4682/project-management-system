package com.pms.projectservice.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        return new ResponseEntity<>(
                buildResponse(HttpStatus.NOT_FOUND, ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());

        return new ResponseEntity<>(
                buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return new ResponseEntity<>(
                buildResponse(HttpStatus.FORBIDDEN, ex.getMessage()),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        return new ResponseEntity<>(
                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);

        return new ResponseEntity<>(
                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ErrorResponse buildResponse(HttpStatus status, String message) {
        return ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}