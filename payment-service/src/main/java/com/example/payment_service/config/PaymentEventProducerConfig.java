package com.example.payment_service.config;

import com.example.payment_service.event.PaymentEvent;
import com.example.payment_service.serializer.MyJsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer-side config for PaymentEvent - this was missing entirely, which
 * is why PaymentEventPublisher couldn't find a KafkaTemplate<String,PaymentEvent>
 * bean to inject.
 */
@Configuration
public class PaymentEventProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, PaymentEvent> paymentEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyJsonSerializer.class);

        // Same reliability settings as order-service's producer.
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, PaymentEvent> paymentEventKafkaTemplate(
            ProducerFactory<String, PaymentEvent> paymentEventProducerFactory) {
        return new KafkaTemplate<>(paymentEventProducerFactory);
    }
}