package com.example.auth_service.exception;

public class UnauthorizedUserAccessException extends RuntimeException {
    public UnauthorizedUserAccessException(Long userId) {
        super("Unauthorized access to user: " + userId);
    }
}