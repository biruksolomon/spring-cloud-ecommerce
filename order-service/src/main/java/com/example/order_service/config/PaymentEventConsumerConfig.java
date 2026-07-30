package com.example.order_service.config;

import com.example.order_service.event.PaymentEvent;
import com.example.order_service.serializer.MyJsonDeserializer;
import com.example.order_service.serializer.MyJsonSerializer;
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
 * Consumes PaymentEvent from the "payment-events" topic (published by
 * payment-service) so order-service can close the saga loop and move an
 * Order out of PENDING. Mirrors notification-service's KafkaConsumerConfig:
 * manual ack, bounded retries, and a dead-letter topic instead of retrying
 * forever or silently dropping a failed status update.
 */
@Configuration
@EnableKafka
public class PaymentEventConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, PaymentEvent> paymentConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Distinct consumer group from payment-service's "payment-group" and
        // notification-service's "notification-group" - this is a different
        // topic (payment-events) so the group name just needs to be unique
        // per logical subscriber of that topic.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-group");

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new MyJsonDeserializer<>(PaymentEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> paymentEventListenerFactory(
            ConsumerFactory<String, PaymentEvent> paymentConsumerFactory,
            DefaultErrorHandler paymentEventErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(paymentConsumerFactory);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);

        factory.setCommonErrorHandler(paymentEventErrorHandler);

        return factory;
    }

    // Plain String/Object producer used only to republish a failed
    // payment-events message onto "payment-events.DLT".
    @Bean
    public ProducerFactory<String, Object> paymentDltProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> paymentDltKafkaTemplate(
            ProducerFactory<String, Object> paymentDltProducerFactory) {
        return new KafkaTemplate<>(paymentDltProducerFactory);
    }

    // After 3 failed attempts (1s apart), stop retrying and hand the
    // message off to the dead-letter topic instead of blocking the
    // partition or silently dropping the status update.
    @Bean
    public DefaultErrorHandler paymentEventErrorHandler(
            KafkaTemplate<String, Object> paymentDltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                paymentDltKafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 2L); // 2 retries, 1s apart

        return new DefaultErrorHandler(recoverer, backOff);
    }
}