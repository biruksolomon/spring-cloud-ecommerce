package com.example.product_service.exception;

/**
 * Wraps any failure talking to Cloudinary (network error, bad
 * credentials, service outage) behind one exception type, so
 * ProductController/GlobalExceptionHandler don't need to know about
 * Cloudinary's own checked IOException or SDK-specific errors.
 */
public class ImageUploadException extends RuntimeException {
    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}