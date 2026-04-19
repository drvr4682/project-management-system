package com.pms.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse buildResponse(String message, int status, String path) {
        return ErrorResponse.builder()
                .message(message)
                .status(status)
                .timestamp(System.currentTimeMillis())
                .path(path)
                .build();
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleUserExists(UserAlreadyExistsException ex,
                                                HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 400, request.getRequestURI()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException ex,
                                                        HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<?> handleInvalidJwt(InvalidJwtException ex,
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
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        return new ResponseEntity<>(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Validation failed")
                        .timestamp(System.currentTimeMillis())
                        .path(request.getRequestURI())
                        .errors(errors)
                        .build(),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Invalid username or password", 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 404, request.getRequestURI()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AuthorizationDeniedException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Access Denied", 403, request.getRequestURI()),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthentication(AuthenticationException ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Unauthorized", 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex,
                                                    HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Something went wrong", 500, request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}