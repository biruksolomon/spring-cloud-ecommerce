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
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
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

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    public void processPayment(OrderCreatedEvent event) {

        // Idempotency guard: if we've already recorded a payment for this
        // orderId (e.g. this is a Kafka-redelivered duplicate of a message
        // we already handled), don't charge again - just stop here.
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
            PaymentIntent intent = charge(event);
            payment.setStripePaymentIntentId(intent.getId());

            // "succeeded" is what a confirmed off_session PaymentIntent
            // settles to immediately in test mode with a test payment
            // method. In a real checkout the customer confirms the
            // PaymentIntent client-side (Stripe Elements) and this service
            // would instead create it unconfirmed and let the
            // payment_intent.succeeded/payment_intent.payment_failed
            // webhook (see PaymentWebhookController) be the source of
            // truth - a card can require 3D Secure or otherwise not
            // settle synchronously.
            payment.setStatus("succeeded".equals(intent.getStatus()) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        } catch (StripeException e) {
            log.error("Stripe charge failed for orderId {}: {}", event.getOrderId(), e.getMessage());
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

    private PaymentIntent charge(OrderCreatedEvent event) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toCents(event.getTotalPrice()))
                .setCurrency("usd")
                // Stripe's built-in test payment method - always succeeds
                // in test mode. A real checkout would instead pass the
                // PaymentMethod id collected from Stripe Elements on the
                // frontend, and would not set confirm/off_session the way
                // this backend-only demo does.
                .setPaymentMethod("pm_card_visa")
                .setConfirm(true)
                .setOffSession(true)
                .putMetadata("orderId", String.valueOf(event.getOrderId()))
                .putMetadata("userId", String.valueOf(event.getUserId()))
                .build();

        return PaymentIntent.create(params);
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

    // Called by PaymentWebhookController when Stripe confirms a
    // PaymentIntent's final status asynchronously (e.g. after 3D Secure
    // completes, for a flow that doesn't settle synchronously like the
    // off_session charge above does). Idempotent: a status that's already
    // terminal is left alone, so a redelivered webhook can't undo it.
    public void applyStripeWebhookResult(String stripePaymentIntentId, boolean succeeded) {
        paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId).ifPresentOrElse(payment -> {
            if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED) {
                log.info("Payment {} already in terminal status {}, ignoring webhook", payment.getPaymentId(), payment.getStatus());
                return;
            }

            PaymentStatus newStatus = succeeded ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payment.setStatus(newStatus);
            paymentRepository.save(payment);

            paymentEventPublisher.publish(new PaymentEvent(
                    payment.getOrderId(),
                    payment.getPaymentId(),
                    newStatus.name()
            ));

            log.info("Payment {} moved to {} via Stripe webhook", payment.getPaymentId(), newStatus);
        }, () -> log.warn("Received Stripe webhook for unknown PaymentIntent {}", stripePaymentIntentId));
    }
}