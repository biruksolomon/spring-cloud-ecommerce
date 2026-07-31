package com.example.payment_service.service;

import com.example.payment_service.publisher.PaymentEventPublisher;
import com.example.payment_service.domain.Payment;
import com.example.payment_service.domain.PaymentStatus;
import com.example.payment_service.dto.OrderCreatedEvent;
import com.example.payment_service.event.PaymentEvent;
import com.example.payment_service.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final Random random = new Random();

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

        // Simulated payment gateway: 80% succeed, 20% fail, so the
        // compensating CANCELLED path is actually exercisable in testing.
        boolean success = event.getTotalPrice() != null
                && event.getTotalPrice() > 0
                && random.nextInt(100) < 80;

        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        Payment saved = paymentRepository.save(payment);

        log.info("Payment {} for orderId {}: {}", saved.getPaymentId(), saved.getOrderId(), saved.getStatus());

        paymentEventPublisher.publish(new PaymentEvent(
                saved.getOrderId(),
                saved.getPaymentId(),
                saved.getStatus().name()
        ));
    }

    // Mirrors OrderService.getOrderById's authorization pattern: a user can
    // only look up a payment that belongs to them.
    public Payment getPaymentByOrderId(Long orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for orderId " + orderId));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to this payment");
        }

        return payment;
    }

    public List<Payment> getPaymentsForUser(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
}