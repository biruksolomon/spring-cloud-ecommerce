package com.example.order_service.publisher;

import com.example.order_service.config.RabbitMQConfig;
import com.example.order_service.event.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OrderCreatedEvent event){
        rabbitTemplate.convertAndSend(   //Java Object to JSON then Send to RabbitMQ
                RabbitMQConfig.EXCHANGE, //send message to order.exchange
                RabbitMQConfig.ROUTING_KEY, // address order.created
                event  // Actual Message

        );
    }
}
