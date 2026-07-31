package com.example.order_service.consumer;

import com.example.order_service.event.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventDeadLetterConsumer {

    @KafkaListener(
            topics = "payment-events.DLT",
            groupId = "order-group-dlt"
    )
    public void readDeadLetter(PaymentEvent event, Acknowledgment acknowledgment) {

        log.error("DLQ payment event received for orderId {} (status {}) — needs manual attention",
                event.getOrderId(), event.getStatus());

        acknowledgment.acknowledge();
    }
}