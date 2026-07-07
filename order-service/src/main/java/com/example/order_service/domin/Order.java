package com.example.order_service.domin;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "orders")
@Builder
@RequiredArgsConstructor
public class Order {
    @Id
    @GeneratedValue
    private Long orderId;

    private Long productId;

    private Integer quantity;

    private Double totalPrice;
}
