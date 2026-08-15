package com.example.order_service.publisher;

import com.example.order_service.event.OrderStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaOrderStatusPublisher {

    @Value("${app.kafka.order-status-topic}")
    private String topic;

    private final KafkaTemplate<String, OrderStatusEvent> kafkaTemplate;

    public KafkaOrderStatusPublisher(KafkaTemplate<String, OrderStatusEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderStatusEvent event) {
        kafkaTemplate.send(topic, event).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published order status event for orderId {}: {}", event.getOrderId(), event.getStatus());
            } else {
                log.error("Failed to publish order status event for orderId {}", event.getOrderId(), ex);
            }
        });
    }
}
