package com.example.order_service.publisher;

import com.example.order_service.event.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderPublisher {

    @Value("${app.kafka.order-topic}")
    private String topic;

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public KafkaOrderPublisher(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(
            OrderCreatedEvent event){

        kafkaTemplate.send(
                topic,
                event
        ).whenComplete(
                (result,ex)->{
                    if(ex==null){
                        System.out.println(
                                "Message Sent"+ result.getRecordMetadata()
                        );
                    }
                    else{
                        System.out.println(
                                ex.getMessage()
                        );
                    }
                }
        );
    }

}