package com.example.order_service.client;

import com.example.order_service.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "product-service"
//        , url = "http://localhost:8081"
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponseDto getProduct(
            @PathVariable Long id);

    @PostMapping("/products/{id}/reserve")
    void reserve(@PathVariable Long id, @RequestParam int quantity);

    @PostMapping("/products/{id}/restore")
    void restore(@PathVariable Long id, @RequestParam int quantity);

}
