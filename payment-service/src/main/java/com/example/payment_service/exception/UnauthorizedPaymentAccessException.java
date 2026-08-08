package com.example.payment_service.exception;

public class UnauthorizedPaymentAccessException extends RuntimeException {
    public UnauthorizedPaymentAccessException(Long orderId) {
        super("Unauthorized access to payment for orderId: " + orderId);
    }
}