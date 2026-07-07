package com.example.order_service.client;

import com.example.order_service.dtos.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "product-service",
        url = "http://localhost:8081"
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponseDto getProduct(
        @PathVariable Long id);

}
