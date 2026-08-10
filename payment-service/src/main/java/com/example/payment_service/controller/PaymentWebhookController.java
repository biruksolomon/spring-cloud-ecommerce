package com.example.payment_service.controller;

import com.example.payment_service.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Receives Stripe's asynchronous notifications about a Checkout
 * Session's outcome. Stripe calls this directly (not through a user's
 * browser), so there is no JWT on this request - it's authenticated
 * instead by the Stripe-Signature header, verified against the webhook
 * signing secret.
 * <p>
 * This path must be added to api-gateway's PUBLIC_ANY_METHOD list (or
 * this service needs to be reachable directly by Stripe, bypassing the
 * gateway) - Stripe cannot supply an Authorization: Bearer token.
 */
@Slf4j
@RestController
@RequestMapping("/payments/webhook")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().reduce("", (a, b) -> a + b);
        String sigHeader = request.getHeader("Stripe-Signature");

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

        switch (event.getType()) {
            // Fired when the customer successfully completes payment on
            // the hosted Checkout page.
            case "checkout.session.completed" -> {
                if (stripeObject instanceof Session session) {
                    paymentService.applyStripeCheckoutResult(session.getId(), session.getPaymentIntent(), true);
                }
            }
            // Fired when the Checkout Session's payment window closes
            // (e.g. 24h) without the customer ever paying.
            case "checkout.session.expired" -> {
                if (stripeObject instanceof Session session) {
                    paymentService.applyStripeCheckoutResult(session.getId(), session.getPaymentIntent(), false);
                }
            }
            default -> log.debug("Ignoring unhandled Stripe event type: {}", event.getType());
        }

        // 200 tells Stripe the event was received - Stripe retries with
        // backoff on anything else, so returning 200 even for an event
        // type we don't act on (default case above) is correct, not a bug.
        return ResponseEntity.ok("received");
    }
}