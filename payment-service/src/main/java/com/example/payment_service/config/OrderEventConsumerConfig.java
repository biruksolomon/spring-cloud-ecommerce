package com.example.payment_service.config;

import com.example.payment_service.dto.OrderCreatedEvent;
import com.example.payment_service.serializer.MyJsonDeserializer;
import com.example.payment_service.serializer.MyJsonSerializer;
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
            ConsumerFactory<String, OrderCreatedEvent> orderConsumerFactory,
            DefaultErrorHandler orderEventErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderConsumerFactory);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);

        factory.setCommonErrorHandler(orderEventErrorHandler);

        return factory;
    }

    // Plain String/Object producer used only to republish a failed
    // order-events message onto "order-events.DLT".
    @Bean
    public ProducerFactory<String, Object> orderEventDltProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> orderEventDltKafkaTemplate(
            ProducerFactory<String, Object> orderEventDltProducerFactory) {
        return new KafkaTemplate<>(orderEventDltProducerFactory);
    }

    // After 3 failed attempts (1s apart), stop retrying and publish to the
    // dead-letter topic instead of just logging to stderr and dropping the
    // message - a poisoned/invalid order-events record should be
    // recoverable, not silently lost.
    @Bean
    public DefaultErrorHandler orderEventErrorHandler(
            KafkaTemplate<String, Object> orderEventDltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                orderEventDltKafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 2L); // 2 retries, 1s apart

        return new DefaultErrorHandler(recoverer, backOff);
    }
}