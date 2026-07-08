package com.example.order_service.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "notification.queue";

    public static final String EXCHANGE = "order.exchange";

    public static final String ROUTING_KEY = "order.created";
}
