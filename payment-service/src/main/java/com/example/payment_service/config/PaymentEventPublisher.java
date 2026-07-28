package com.example.payment_service.config;

import com.example.payment_service.event.PaymentEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    @Value("${app.kafka.payment-topic}")
    private String topic;

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PaymentEvent event) {
        kafkaTemplate.send(topic, event).whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Payment event sent: " + result.getRecordMetadata());
            } else {
                System.out.println("Failed to send payment event: " + ex.getMessage());
            }
        });
    }
}
