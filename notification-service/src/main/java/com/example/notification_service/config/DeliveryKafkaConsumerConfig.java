package com.example.notification_service.config;

import com.example.notification_service.dto.DeliveryStatusEvent;
import com.example.notification_service.serializer.MyJsonDeserializer;
import com.example.notification_service.serializer.MyJsonSerializer;
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
 * Mirrors KafkaConsumerConfig but subscribes to "delivery-events"
 * (published by delivery-service) instead of "order-events" - kept
 * separate since Spring Kafka's ConsumerFactory/listener-container-factory
 * pair is typed to a single value class.
 */
@Configuration
@EnableKafka
public class DeliveryKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, DeliveryStatusEvent> deliveryConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-delivery-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new MyJsonDeserializer<>(DeliveryStatusEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryStatusEvent> deliveryKafkaListenerContainerFactory(
            ConsumerFactory<String, DeliveryStatusEvent> deliveryConsumerFactory,
            DefaultErrorHandler deliveryErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, DeliveryStatusEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(deliveryConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(deliveryErrorHandler);

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
    public DefaultErrorHandler deliveryErrorHandler(
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
