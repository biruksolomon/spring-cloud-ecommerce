package com.example.notification_service.consumer;

import com.example.notification_service.config.RabbitMQConfig;
import com.example.notification_service.dto.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterConsumer {

    @RabbitListener(
            queues = RabbitMQConfig.DLQ
    )
    public void readDeadLetter(
            OrderCreatedEvent event){

        System.out.println(

                "DLQ Message : "

                        + event.getOrderId()

        );

    }

}
