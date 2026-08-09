package com.example.payment_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sets the Stripe SDK's global API key on startup. stripe-java is
 * effectively a static client (Stripe.apiKey is a static field the whole
 * SDK reads from), so this just needs to run once.
 */
@Component
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}