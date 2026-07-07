package com.example.product_service.service;

import com.example.product_service.domain.Product;
import com.example.product_service.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    ProductService(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    public Product create(Product product){return productRepository.save(product);}

    public Page<Product> getAllProducts(int page , int size){
        Pageable pageable = PageRequest.of(page,size);

        return productRepository.findAll(pageable);

    }

    public Product getProduct(Long productId){return productRepository.findById(productId).orElseThrow(()-> new RuntimeException("product not found )"));}

}
