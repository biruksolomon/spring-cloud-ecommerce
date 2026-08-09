package com.example.auth_service.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException() {
        super("User account is inactive");
    }
}