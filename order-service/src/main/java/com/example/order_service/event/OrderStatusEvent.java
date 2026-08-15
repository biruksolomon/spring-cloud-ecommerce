package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Published to the "order-status-events" topic every time an order's
 * status changes (payment result applied, or an admin override). Distinct
 * from OrderCreatedEvent (which only ever fires once, at creation) so
 * downstream consumers that only care about status transitions - notably
 * delivery-service, which starts processing a shipment once an order goes
 * CONFIRMED and cancels it if the order goes CANCELLED - don't have to
 * replay the whole order-events topic and diff it themselves.
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
