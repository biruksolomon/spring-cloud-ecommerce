package com.example.delivery_service.exception;

import com.example.delivery_service.domain.DeliveryStatus;

public class InvalidDeliveryStatusTransitionException extends RuntimeException {
    public InvalidDeliveryStatusTransitionException(DeliveryStatus from, DeliveryStatus to) {
        super("Cannot move delivery from " + from + " to " + to);
    }
}
