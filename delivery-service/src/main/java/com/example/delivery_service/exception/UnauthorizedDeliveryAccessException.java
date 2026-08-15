package com.example.delivery_service.exception;

public class UnauthorizedDeliveryAccessException extends RuntimeException {
    public UnauthorizedDeliveryAccessException(Long deliveryId) {
        super("Unauthorized access to delivery: " + deliveryId);
    }
}
