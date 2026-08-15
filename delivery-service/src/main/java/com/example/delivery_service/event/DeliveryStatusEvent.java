package com.example.delivery_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Published to the "delivery-events" topic every time a delivery's status
 * changes. Consumed by order-service (to reflect shipment progress on the
 * order) and notification-service (to tell the customer their package
 * moved).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryStatusEvent {

    private Long deliveryId;

    private Long orderId;

    private Long userId;

    private String status;

    private String location;

    private Long updatedAt;
}
