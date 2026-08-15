package com.example.order_service.config;

import com.example.order_service.event.DeliveryStatusEvent;
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
 * Consumes DeliveryStatusEvent from the "delivery-events" topic (published
 * by delivery-service) so order-service can reflect shipment progress
 * (SHIPPED, DELIVERED) on the order itself. Mirrors
 * PaymentEventConsumerConfig: manual ack, bounded retries, dead-letter
 * topic on repeated failure.
 */
@Configuration
@EnableKafka
public class DeliveryEventConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, DeliveryStatusEvent> deliveryConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Distinct consumer group from every other subscriber of any
        // topic in this system - "order-group" is already used for
        // payment-events, so this needs its own name even though it's the
        // same service, since group id is scoped per-topic-subscription
        // pattern rather than per-service.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-delivery-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new MyJsonDeserializer<>(DeliveryStatusEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryStatusEvent> deliveryEventListenerFactory(
            ConsumerFactory<String, DeliveryStatusEvent> deliveryConsumerFactory,
            DefaultErrorHandler deliveryEventErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, DeliveryStatusEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(deliveryConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(deliveryEventErrorHandler);

        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> deliveryDltProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> deliveryDltKafkaTemplate(
            ProducerFactory<String, Object> deliveryDltProducerFactory) {
        return new KafkaTemplate<>(deliveryDltProducerFactory);
    }

    @Bean
    public DefaultErrorHandler deliveryEventErrorHandler(
            KafkaTemplate<String, Object> deliveryDltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deliveryDltKafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
