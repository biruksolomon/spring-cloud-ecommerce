package com.example.product_service.config;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the Cloudinary SDK client from cloudinary.* properties (see
 * application.yml), which in turn read CLOUDINARY_CLOUD_NAME/API_KEY/
 * API_SECRET from the environment - see .env.example at the repo root.
 * <p>
 * Left unconfigured (empty strings) on purpose when the env vars aren't
 * set, same pattern as GoogleTokenVerifier in auth-service: the bean
 * still builds, but CloudinaryImageStorageService.upload() fails fast
 * with a clear error the first time it's actually used, rather than the
 * app crashing on startup for services that never touch image upload
 * (e.g. running product-service locally without Cloudinary configured).
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}