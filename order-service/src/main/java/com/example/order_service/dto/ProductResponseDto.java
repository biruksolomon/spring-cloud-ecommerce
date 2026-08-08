package com.example.order_service.dto;

import lombok.Data;

@Data
public class ProductResponseDto {

    private Long productId;

    private String productName;

    private String description;

    private Double price;

    private Integer quantity;
}
