package com.example.auth_service.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        // Deliberately generic - same message whether the token is
        // unknown, expired, or already revoked, so a caller can't probe
        // which case they hit.
        super("Refresh token is invalid or expired");
    }
}
