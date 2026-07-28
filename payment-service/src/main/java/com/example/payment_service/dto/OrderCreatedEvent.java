package com.example.payment_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreatedEvent {

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private Double totalPrice;
}
