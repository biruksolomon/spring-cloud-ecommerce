package com.example.order_service.event;

import com.example.order_service.dto.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Published to the "order-events" topic when an order is created (status
 * PENDING, before payment). Carries both the shipment destination
 * (customer's delivery address) and origin (product's warehouse address)
 * so delivery-service can open a shipment record without an extra call
 * back into order-service or product-service.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private Double totalPrice;

    // Customer's shipping destination.
    private AddressDto deliveryAddress;

    // Product's warehouse/origin address, as known by product-service at
    // the time the order was placed. May have null fields if the admin
    // never set one on the product.
    private AddressDto originAddress;
}