package com.example.notification_service.config;

import com.example.notification_service.dto.OrderCreatedEvent;
import com.example.notification_service.serializer.MyJsonDeserializer;
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
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> consumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId);

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new MyJsonDeserializer<>(OrderCreatedEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> consumerFactory,
            DefaultErrorHandler defaultErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL);

        factory.setCommonErrorHandler(defaultErrorHandler);

        return factory;
    }

    // A plain String/Object producer used only to republish a failed
    // message onto its dead-letter topic (e.g. "order-events.DLT").
    @Bean
    public ProducerFactory<String, Object> dltProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                MyJsonDeserializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
            ProducerFactory<String, Object> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    // After 3 failed attempts (1s apart), stop retrying and hand the
    // message off to the recoverer below instead of blocking the partition.
    @Bean
    public DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<String, Object> dltKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition())
        );

        FixedBackOff backOff = new FixedBackOff(1000L, 2L); // 2 retries, 1s apart

        return new DefaultErrorHandler(recoverer, backOff);
    }
}