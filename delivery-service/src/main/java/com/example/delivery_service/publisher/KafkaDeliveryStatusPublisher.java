package com.example.delivery_service.publisher;

import com.example.delivery_service.event.DeliveryStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaDeliveryStatusPublisher {

    @Value("${app.kafka.delivery-status-topic}")
    private String topic;

    private final KafkaTemplate<String, DeliveryStatusEvent> kafkaTemplate;

    public KafkaDeliveryStatusPublisher(KafkaTemplate<String, DeliveryStatusEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DeliveryStatusEvent event) {
        kafkaTemplate.send(topic, event).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published delivery status event for deliveryId {} (orderId {}): {}",
                        event.getDeliveryId(), event.getOrderId(), event.getStatus());
            } else {
                log.error("Failed to publish delivery status event for deliveryId {}", event.getDeliveryId(), ex);
            }
        });
    }
}
