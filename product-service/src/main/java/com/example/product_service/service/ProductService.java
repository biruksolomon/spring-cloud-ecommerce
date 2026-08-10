package com.example.product_service.service;

import com.example.product_service.domain.Product;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.exception.ProductNotFoundException;
import com.example.product_service.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.product_service.exception.InsufficientStockException;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(ProductRequest request) {
        Product product = new Product();
        // productId is intentionally never taken from the request - the DB
        // generates it, so an admin can't overwrite an existing product by id.
        applyRequest(product, request);
        return productRepository.save(product);
    }

    public Product update(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        applyRequest(product, request);
        return productRepository.save(product);
    }

    public void delete(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
    }

    public Page<Product> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable);
    }

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        validateQuantity(quantity);
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        if (productRepository.reserve(productId, quantity) == 0) {
            throw new InsufficientStockException(productId, quantity);
        }
    }

    @Transactional
    public void restore(Long productId, int quantity) {
        validateQuantity(quantity);
        if (productRepository.restore(productId, quantity) == 0) {
            throw new ProductNotFoundException(productId);
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
    }
}
