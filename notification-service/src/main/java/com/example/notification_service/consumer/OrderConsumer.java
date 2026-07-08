package com.example.notification_service.consumer;


import com.example.notification_service.config.RabbitMQConfig;
import com.example.notification_service.dto.OrderCreatedEvent;
import com.example.notification_service.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {
    private final NotificationService notificationService;

    public OrderConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE
    )
    public void receive(OrderCreatedEvent event){

        notificationService.saveNotification(event);

    }
}
