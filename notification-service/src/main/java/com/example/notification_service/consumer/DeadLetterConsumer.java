package com.example.notification_service.consumer;

//import com.example.notification_service.config.RabbitMQConfig;
import com.example.notification_service.dto.OrderCreatedEvent;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeadLetterConsumer {

    /*@RabbitListener(
            queues = RabbitMQConfig.DLQ
    )*/
    public void readDeadLetter(
            OrderCreatedEvent event){

        System.out.println(

                "DLQ Message : "

                        + event.getOrderId()

        );

    }

    @KafkaListener(
            topics = "order-events.DLT",
            groupId = "notification-group-dlt"
    )
    public void readDeadLetter(OrderCreatedEvent event, Acknowledgment acknowledgment) {

        log.error("DLQ message received for orderId {} — needs manual attention", event.getOrderId());

        acknowledgment.acknowledge();
    }

}
