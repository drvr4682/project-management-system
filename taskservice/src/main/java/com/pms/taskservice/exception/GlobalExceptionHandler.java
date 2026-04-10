package com.pms.taskservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // VALIDATION ERRORS → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 400,
                        "errors", errors
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    // ILLEGAL ARGUMENT → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 400,
                        "message", ex.getMessage()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    // RUNTIME (BUSINESS) → 403 / 404 (TEMP → we refine later)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 500,
                        "message", ex.getMessage()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(com.pms.taskservice.exception.ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(com.pms.taskservice.exception.ResourceNotFoundException ex) {

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 404,
                        "message", ex.getMessage()
                ),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(com.pms.taskservice.exception.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(com.pms.taskservice.exception.AccessDeniedException ex) {

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 403,
                        "message", ex.getMessage()
                ),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(com.pms.taskservice.exception.ServiceUnavailableException.class)
    public ResponseEntity<?> handleServiceUnavailable(com.pms.taskservice.exception.ServiceUnavailableException ex) {

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 503,
                        "message", ex.getMessage()
                ),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    // FALLBACK → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception ex) {

        return new ResponseEntity<>(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 500,
                        "message", ex.getMessage()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}