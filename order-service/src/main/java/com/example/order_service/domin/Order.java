package com.example.order_service.domin;


import jakarta.persistence.*;
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

    private Long productId;

    private Integer quantity;

    private Double totalPrice;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = false, updatable = false)
    private Long createdAt = System.currentTimeMillis();

    @Column
    private Long updatedAt;
}
