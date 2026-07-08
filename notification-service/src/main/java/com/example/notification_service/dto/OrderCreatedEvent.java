package com.example.notification_service.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderCreatedEvent {

    private Long orderId;

    private Long productId;

    private Integer quantity;

    private Double totalPrice;
}
