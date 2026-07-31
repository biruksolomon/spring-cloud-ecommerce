package com.example.payment_service.consumer;

import com.example.payment_service.dto.OrderCreatedEvent;
import com.example.payment_service.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;

    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "payment-group",
            containerFactory = "orderEventListenerFactory"
    )
    public void receive(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Payment service received orderId {}", event.getOrderId());
        paymentService.processPayment(event);
        acknowledgment.acknowledge();
    }
}