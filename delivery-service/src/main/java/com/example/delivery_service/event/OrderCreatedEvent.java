package com.example.delivery_service.event;

import com.example.delivery_service.dto.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors order-service's OrderCreatedEvent field-for-field. Consumed from
 * the "order-events" topic to open a shipment (status PENDING) as soon as
 * an order is placed, using the origin/destination addresses order-service
 * already resolved.
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

    private AddressDto deliveryAddress;

    private AddressDto originAddress;
}
