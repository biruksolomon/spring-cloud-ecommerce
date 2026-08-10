package com.example.order_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductClientConfig {

    @Bean
    RequestInterceptor internalServiceTokenInterceptor(
            @Value("${app.internal-service-token}") String internalServiceToken) {
        return template -> template.header("X-Internal-Service-Token", internalServiceToken);
    }
}
