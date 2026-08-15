package com.example.order_service.dto;

import lombok.Data;

@Data
public class ProductResponseDto {

    private Long productId;

    private String productName;

    private String description;

    private Double price;

    private Integer quantity;

    // Warehouse/origin address the product ships from - forwarded onto
    // the OrderCreatedEvent so delivery-service knows where a shipment
    // originates without calling product-service itself.
    private AddressDto address;
}