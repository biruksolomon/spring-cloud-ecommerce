package com.example.product_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.product_service.exception.ImageUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * The only class in product-service that talks to Cloudinary. Everything
 * else (ProductService, ProductController) works with plain URLs/IDs, so
 * swapping storage providers later only means rewriting this one class.
 */
@Slf4j
@Service
public class CloudinaryImageStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Validates and uploads one product image. public_id is derived from
     * the product id (plus a timestamp, so repeated uploads for the same
     * product don't collide with a CDN-cached copy of the old one) rather
     * than left to Cloudinary to generate, so ProductService can find and
     * delete the previous asset on re-upload.
     */
    public UploadedImage upload(MultipartFile file, Long productId) {
        validate(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "products",
                    "public_id", "product-" + productId + "-" + System.currentTimeMillis(),
                    "resource_type", "image"
            ));
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            if (url == null || publicId == null) {
                throw new ImageUploadException("Cloudinary returned an unexpected response");
            }
            return new UploadedImage(url, publicId);
        } catch (IOException e) {
            throw new ImageUploadException("Failed to reach Cloudinary", e);
        }
    }

    /**
     * Best-effort delete of a previous asset (called after a successful
     * re-upload, or when a product is deleted). Failures are logged, not
     * thrown - losing track of one orphaned Cloudinary asset shouldn't
     * fail the request that's already succeeded on our side.
     */
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", publicId, e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("image must be 5MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("image must be JPEG, PNG, or WEBP");
        }
    }

    public record UploadedImage(String url, String publicId) {
    }
}