package com.example.order_service.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found: " + orderId);
    }
}