package com.example.order_service.domin;


import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private Integer quantity;

    private Double totalPrice;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false, updatable = false)
    private Long createdAt = System.currentTimeMillis();

    @Column
    private Long updatedAt;

    // Where this order should be shipped. Required on creation so
    // delivery-service always has a destination to open a shipment
    // against - see OrderController#createOrder (@Valid).
    @Valid
    @NotNull(message = "deliveryAddress is required")
    @Embedded
    private DeliveryAddress deliveryAddress;
}