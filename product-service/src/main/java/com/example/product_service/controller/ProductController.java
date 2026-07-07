package com.example.product_service.controller;


import com.example.product_service.domain.Product;
import com.example.product_service.service.ProductService;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Product save(@RequestBody Product product){
        return productService.create(product);
    }

    @GetMapping
    public Page<Product> getAllProducts(
            @RequestParam int page,
            @RequestParam int size){
        return productService.getAllProducts(page,size);
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id){

        return productService.getProduct(id);
    }


}
