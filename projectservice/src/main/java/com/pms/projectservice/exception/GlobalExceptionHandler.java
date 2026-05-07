package com.pms.projectservice.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String sanitize(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input.replaceAll("[\r\n]", ""));
    }

    private ErrorResponse buildResponse(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
                .status(status.value())
                .message(sanitize(message))
                .timestamp(System.currentTimeMillis())
                .path(sanitize(path))
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                        HttpServletRequest request) {
        log.error("Resource not found: {}", sanitize(ex.getMessage()));

        return new ResponseEntity<>(
                buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), sanitize(request.getRequestURI())),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex,
                                                                HttpServletRequest request) {
        log.warn("Unauthorized: {}", sanitize(ex.getMessage()));

        return new ResponseEntity<>(
                buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), sanitize(request.getRequestURI())),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        log.warn("Access denied: {}", sanitize(ex.getMessage()));

        return new ResponseEntity<>(
                buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), sanitize(request.getRequestURI())),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(sanitize(e.getField()), sanitize(e.getDefaultMessage())));

        return new ResponseEntity<>(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Validation failed")
                        .timestamp(System.currentTimeMillis())
                        .path(sanitize(request.getRequestURI()))
                        .errors(errors)
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex,
                                                            HttpServletRequest request) {
        log.warn("Bad request: {}", sanitize(ex.getMessage()));

        return new ResponseEntity<>(
                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), sanitize(request.getRequestURI())),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(AuthorizationDeniedException ex,
                                                                    HttpServletRequest request) {

        return new ResponseEntity<>(
                buildResponse(
                        HttpStatus.FORBIDDEN,
                        "Access Denied",
                        sanitize(request.getRequestURI())
                ),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
                                                        HttpServletRequest request) {
        log.error("Unexpected error: ", ex);

        return new ResponseEntity<>(
                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", sanitize(request.getRequestURI())),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex,
                                                                    HttpServletRequest request) {

        log.error("Service unavailable: {}", sanitize(ex.getMessage()));

        return new ResponseEntity<>(
                buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), sanitize(request.getRequestURI())),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}