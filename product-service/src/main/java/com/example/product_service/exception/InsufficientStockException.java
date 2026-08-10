package com.example.product_service.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int quantity) {
        super("Insufficient stock for product " + productId + " (requested " + quantity + ")");
    }
}
