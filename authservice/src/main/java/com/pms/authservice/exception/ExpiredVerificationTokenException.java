package com.pms.authservice.exception;

public class ExpiredVerificationTokenException extends RuntimeException {
    public ExpiredVerificationTokenException(String message) {
        super(message);
    }
}
