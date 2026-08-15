package com.example.delivery_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors order-service's OrderStatusEvent. Consumed from the
 * "order-status-events" topic so a shipment can start processing once its
 * order is CONFIRMED (payment succeeded) and be cancelled if its order is
 * CANCELLED - a shipment should never be dispatched for an order that was
 * never paid for.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusEvent {

    private Long orderId;

    private Long userId;

    private String status;

    private Long updatedAt;
}
