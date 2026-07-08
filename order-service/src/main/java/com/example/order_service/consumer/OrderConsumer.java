package com.example.order_service.consumer;

import com.example.order_service.config.RabbitMQConfig;
import com.example.order_service.event.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


/*
*
*   Testing as a Consumer
*
* */
@Component
public class OrderConsumer {

   /* @RabbitListener(
            queues = RabbitMQConfig.QUEUE
    )*/
    public void receive(
            OrderCreatedEvent event){

        System.out.println(
                "Received Order : "
                        + event.getOrderId()
        );

    }

}
