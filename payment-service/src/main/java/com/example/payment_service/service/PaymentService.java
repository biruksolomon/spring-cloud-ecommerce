package com.example.payment_service.service;

import com.example.payment_service.publisher.PaymentEventPublisher;
import com.example.payment_service.domain.Payment;
import com.example.payment_service.domain.PaymentStatus;
import com.example.payment_service.dto.OrderCreatedEvent;
import com.example.payment_service.event.PaymentEvent;
import com.example.payment_service.exception.PaymentNotFoundException;
import com.example.payment_service.exception.UnauthorizedPaymentAccessException;
import com.example.payment_service.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    // Where Stripe sends the customer back to after they pay/cancel on
    // the hosted Checkout page. Stripe appends its own query params, so
    // the app-provided URLs are just a base page in the frontend.
    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    public void processPayment(OrderCreatedEvent event) {

        // Idempotency guard: if we've already recorded a payment for this
        // orderId (e.g. this is a Kafka-redelivered duplicate of a message
        // we already handled), don't create a second checkout session -
        // just stop here.
        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.info("Payment already processed for orderId {}, skipping", event.getOrderId());
            return;
        }

        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setUserId(event.getUserId());
        payment.setAmount(event.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);

        try {
            Session session = createCheckoutSession(event);
            payment.setStripeCheckoutSessionId(session.getId());
            payment.setCheckoutUrl(session.getUrl());

            // Unlike an auto-confirmed, backend-only charge, a Checkout
            // Session doesn't settle synchronously - the customer still
            // has to open checkoutUrl and pay. Status stays PENDING until
            // the checkout.session.completed webhook (see
            // PaymentWebhookController) reports the outcome.
            payment.setStatus(PaymentStatus.PENDING);
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for orderId {}: {}", event.getOrderId(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Payment {} for orderId {}: {}", saved.getPaymentId(), saved.getOrderId(), saved.getStatus());

        paymentEventPublisher.publish(new PaymentEvent(
                saved.getOrderId(),
                saved.getPaymentId(),
                saved.getStatus().name()
        ));
    }

    private Session createCheckoutSession(OrderCreatedEvent event) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?orderId=" + event.getOrderId())
                .setCancelUrl(cancelUrl + "?orderId=" + event.getOrderId())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(toCents(event.getTotalPrice()))
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Order #" + event.getOrderId())
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("orderId", String.valueOf(event.getOrderId()))
                .putMetadata("userId", String.valueOf(event.getUserId()))
                .build();

        return Session.create(params);
    }

    // Stripe wants a whole-number amount in the currency's smallest unit
    // (cents for USD), not a decimal dollar amount.
    private long toCents(Double amount) {
        if (amount == null) {
            return 0L;
        }
        return BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
    }

    // Mirrors OrderService.getOrderById's authorization pattern: a user can
    // only look up a payment that belongs to them.
    public Payment getPaymentByOrderId(Long orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (!payment.getUserId().equals(userId)) {
            throw new UnauthorizedPaymentAccessException(orderId);
        }

        return payment;
    }

    public List<Payment> getPaymentsForUser(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    // Admin-only: every payment in the system, optionally filtered by
    // status (e.g. FAILED, for a support queue). Guarded by
    // @RequireRole(ADMIN) on the controller method, not by any check here.
    public Page<Payment> getAllPayments(PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return paymentRepository.findByStatus(status, pageable);
        }
        return paymentRepository.findAll(pageable);
    }

    // Called by PaymentWebhookController once Stripe reports a
    // Checkout Session's outcome (checkout.session.completed /
    // checkout.session.expired). Looks the payment up by the session id
    // we stored when the session was created, records the PaymentIntent
    // id Stripe generated behind the scenes (needed if we ever get a
    // payment_intent.* event for the same charge), and moves the status
    // to a terminal state. Idempotent: a status that's already terminal
    // is left alone, so a redelivered webhook can't undo it.
    public void applyStripeCheckoutResult(String stripeCheckoutSessionId, String stripePaymentIntentId, boolean succeeded) {
        paymentRepository.findByStripeCheckoutSessionId(stripeCheckoutSessionId).ifPresentOrElse(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED) {
                log.info("Payment {} already in terminal status {}, ignoring webhook", payment.getPaymentId(), payment.getStatus());
                return;
            }

            if (stripePaymentIntentId != null) {
                payment.setStripePaymentIntentId(stripePaymentIntentId);
            }

            PaymentStatus newStatus = succeeded ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payment.setStatus(newStatus);
            paymentRepository.save(payment);

            paymentEventPublisher.publish(new PaymentEvent(
                    payment.getOrderId(),
                    payment.getPaymentId(),
                    newStatus.name()
            ));

            log.info("Payment {} moved to {} via Stripe checkout webhook", payment.getPaymentId(), newStatus);
        }, () -> log.warn("Received Stripe webhook for unknown checkout session {}", stripeCheckoutSessionId));
    }
}