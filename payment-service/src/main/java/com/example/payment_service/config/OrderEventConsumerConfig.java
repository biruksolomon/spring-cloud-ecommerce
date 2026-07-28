package com.example.payment_service.config;

import com.example.payment_service.dto.OrderCreatedEvent;
import com.example.payment_service.serializer.MyJsonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class OrderEventConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // A distinct consumer group from notification-service's "notification-group" -
        // Kafka delivers each message to every group independently, so both services
        // see every OrderCreatedEvent without stepping on each other.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new MyJsonDeserializer<>(OrderCreatedEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderEventListenerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory);

        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, ex) -> System.err.println(
                        "Failed to process order-events record after retries: " + record.value() + " - " + ex.getMessage()),
                new FixedBackOff(1000L, 3L)
        ));

        return factory;
    }
}
