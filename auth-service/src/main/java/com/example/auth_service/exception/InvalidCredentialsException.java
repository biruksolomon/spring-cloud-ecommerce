package com.example.auth_service.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        // Deliberately generic message - doesn't reveal whether the email
        // exists, only that email+password together didn't match.
        super("Invalid email or password");
    }
}