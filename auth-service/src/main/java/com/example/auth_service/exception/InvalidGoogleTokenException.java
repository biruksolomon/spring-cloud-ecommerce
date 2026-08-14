package com.example.auth_service.exception;

public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException() {
        super("Invalid or expired Google ID token");
    }
}