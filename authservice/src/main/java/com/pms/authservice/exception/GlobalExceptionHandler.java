package com.pms.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String sanitize(String input) {
        if (input == null) return null;
        return HtmlUtils.htmlEscape(input.replaceAll("[\r\n]", ""));
    }

    private ErrorResponse buildResponse(String message, int status, String path) {
        return ErrorResponse.builder()
                .message(sanitize(message))
                .status(status)
                .timestamp(System.currentTimeMillis())
                .path(sanitize(path))
                .build();
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex,
                                                HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 400, request.getRequestURI()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                        HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(InvalidJwtException ex,
                                                HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                    HttpServletRequest request) {
        log.warn("Bad credentials attempt for path: {}", sanitize(request.getRequestURI()));
        return new ResponseEntity<>(
                buildResponse("Invalid username or password", 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 404, sanitize(request.getRequestURI())),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AuthorizationDeniedException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Access Denied", 403, sanitize(request.getRequestURI())),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Unauthorized", 401, sanitize(request.getRequestURI())),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Something went wrong", 500, sanitize(request.getRequestURI())),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}