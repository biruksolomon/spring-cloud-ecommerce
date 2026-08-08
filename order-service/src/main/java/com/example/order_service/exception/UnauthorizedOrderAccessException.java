package com.example.order_service.exception;

public class UnauthorizedOrderAccessException extends RuntimeException {
    public UnauthorizedOrderAccessException(Long orderId) {
        super("Unauthorized access to order: " + orderId);
    }
}