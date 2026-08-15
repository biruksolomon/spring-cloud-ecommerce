package com.example.delivery_service.config;

import com.example.delivery_service.event.OrderCreatedEvent;
import com.example.delivery_service.serializer.MyJsonDeserializer;
import com.example.delivery_service.serializer.MyJsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumes OrderCreatedEvent from "order-events" (published by
 * order-service at order creation) to open a new shipment. Mirrors the
 * manual-ack + dead-letter pattern used by every other consumer config in
 * this system.
 */
@Configuration
@EnableKafka
public class OrderCreatedConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "delivery-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new MyJsonDeserializer<>(OrderCreatedEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedListenerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory,
            DefaultErrorHandler orderCreatedErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderCreatedConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(orderCreatedErrorHandler);

        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> orderCreatedDltProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> orderCreatedDltKafkaTemplate(
            ProducerFactory<String, Object> orderCreatedDltProducerFactory) {
        return new KafkaTemplate<>(orderCreatedDltProducerFactory);
    }

    @Bean
    public DefaultErrorHandler orderCreatedErrorHandler(
            KafkaTemplate<String, Object> orderCreatedDltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                orderCreatedDltKafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
