package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors delivery-service's own DeliveryStatusEvent. Consumed from the
 * "delivery-events" topic so order-service can reflect shipment progress
 * (SHIPPED, DELIVERED) on the order itself without delivery-service ever
 * needing to know about order-service's internal status model.
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

    private Long updatedAt;
}
