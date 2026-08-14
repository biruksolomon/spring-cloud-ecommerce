package com.example.product_service.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long productId;

    private String productName;

    private String description;

    private Double price;

    private Integer quantity;

    // Cloudinary's secure_url for this product's image - what clients
    // actually display. Null until an image is uploaded via
    // POST /products/{id}/image.
    private String imageUrl;

    // Cloudinary's public_id for the uploaded asset - not shown to
    // clients (see @JsonIgnore below), kept only so a later re-upload or
    // product deletion can tell Cloudinary which asset to replace/remove.
    @JsonIgnore
    private String imagePublicId;

}