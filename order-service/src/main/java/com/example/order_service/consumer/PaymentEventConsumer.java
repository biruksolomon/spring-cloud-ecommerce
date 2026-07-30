package com.example.order_service.consumer;

import com.example.order_service.event.PaymentEvent;
import com.example.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "payment-events",
            groupId = "order-group",
            containerFactory = "paymentEventListenerFactory"
    )
    public void receive(PaymentEvent event, Acknowledgment acknowledgment) {

        log.info("Order service received payment event for orderId {}: {}",
                event.getOrderId(), event.getStatus());

        orderService.applyPaymentResult(event.getOrderId(), event.getStatus());

        // Only ack after the status update has been persisted - if
        // applyPaymentResult throws, the message is redelivered instead of
        // silently being marked as processed.
        acknowledgment.acknowledge();
    }
}