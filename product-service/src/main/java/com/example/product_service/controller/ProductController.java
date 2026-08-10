package com.example.product_service.controller;


import com.example.product_service.domain.Product;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.security.RequireRole;
import com.example.product_service.security.Role;
import com.example.product_service.service.ProductService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @RequireRole(Role.ADMIN)
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product created = productService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @RequireRole(Role.ADMIN)
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @RequireRole(Role.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Reads are intentionally left unannotated - the gateway allows GET
    // /products without a token, so product-service must not demand a role
    // here or every anonymous browse request would start failing.
    @GetMapping
    public Page<Product> getAllProducts(
            @RequestParam int page,
            @RequestParam int size) {
        return productService.getAllProducts(page, size);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable Long id, @RequestParam int quantity) {
        productService.reserve(id, quantity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id, @RequestParam int quantity) {
        productService.restore(id, quantity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

}