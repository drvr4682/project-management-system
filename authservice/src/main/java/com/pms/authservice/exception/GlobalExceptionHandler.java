package com.pms.authservice.exception;

import com.pms.common.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public ResponseEntity<ErrorResponse> handleUserExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 409, request.getRequestURI()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            InvalidJwtException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 404, request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ErrorResponse> handleEmailVerification(
            EmailVerificationException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 403, request.getRequestURI()),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationToken(
            InvalidVerificationTokenException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 400, request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExpiredVerificationTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredVerificationToken(
            ExpiredVerificationTokenException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 400, request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ErrorResponse> handleEmailSending(
            EmailSendingException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse(ex.getMessage(), 500, request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayload(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String message = "Invalid request payload";
        Throwable cause = ex.getCause();

        if (cause != null && cause.getMessage() != null
                && cause.getMessage().contains("com.pms.authservice.entity.Role")) {
            message = "Invalid role. Allowed values are ADMIN or USER";
        }

        return new ResponseEntity<>(
                buildResponse(message, HttpStatus.BAD_REQUEST.value(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

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
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Invalid username or password", 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AuthorizationDeniedException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Access Denied", 403, request.getRequestURI()),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Unauthorized", 401, request.getRequestURI()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Required header is missing", 400, request.getRequestURI()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                buildResponse("Something went wrong", 500, request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}