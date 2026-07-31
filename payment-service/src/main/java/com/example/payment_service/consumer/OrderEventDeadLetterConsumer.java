package com.example.payment_service.consumer;

import com.example.payment_service.dto.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventDeadLetterConsumer {

    @KafkaListener(
            topics = "order-events.DLT",
            groupId = "payment-group-dlt"
    )
    public void readDeadLetter(OrderCreatedEvent event, Acknowledgment acknowledgment) {

        log.error("DLQ order event received for orderId {} — payment was never attempted, needs manual attention",
                event.getOrderId());

        acknowledgment.acknowledge();
    }
}