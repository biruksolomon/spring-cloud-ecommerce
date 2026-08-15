package com.example.delivery_service.exception;

public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(Long deliveryId) {
        super("Delivery not found: " + deliveryId);
    }

    private DeliveryNotFoundException(String message) {
        super(message);
    }

    public static DeliveryNotFoundException forOrder(Long orderId) {
        return new DeliveryNotFoundException("Delivery not found for order: " + orderId);
    }
}
