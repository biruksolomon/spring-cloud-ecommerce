package com.example.notification_service.consumer;


import com.example.notification_service.config.RabbitMQConfig;
import com.example.notification_service.dto.OrderCreatedEvent;
import com.example.notification_service.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderConsumer {
    private final NotificationService notificationService;

    public OrderConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE
    )
    public void receive(OrderCreatedEvent event){

        log.info("Consumer receive orderId {}",event.getOrderId());

        notificationService.saveNotification(event);

    }
}
