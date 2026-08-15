package com.example.delivery_service.domain;

/**
 * Lifecycle of a shipment, from the moment an order is placed through to
 * the package reaching the customer (or failing to).
 * <p>
 * PENDING     - order placed, awaiting payment confirmation (order-events).
 * PROCESSING  - payment confirmed (order-status-events: CONFIRMED), warehouse can pack it.
 * DISPATCHED  - handed to a carrier, tracking number assigned.
 * IN_TRANSIT  - carrier has scanned it en route.
 * OUT_FOR_DELIVERY - on the final leg to the recipient.
 * DELIVERED   - received by the customer. Terminal.
 * FAILED      - delivery attempt failed (e.g. recipient unreachable).
 * RETURNED    - sent back to origin after a failed delivery. Terminal.
 * CANCELLED   - order was cancelled before/while in transit. Terminal.
 */
public enum DeliveryStatus {
    PENDING,
    PROCESSING,
    DISPATCHED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED,
    CANCELLED
}
