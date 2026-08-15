package com.example.delivery_service.consumer;

import com.example.delivery_service.event.OrderCreatedEvent;
import com.example.delivery_service.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCreatedConsumer {

    private final DeliveryService deliveryService;

    public OrderCreatedConsumer(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = "order-events",
            groupId = "delivery-group",
            containerFactory = "orderCreatedListenerFactory"
    )
    public void receive(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Delivery service received order-created event for orderId {}", event.getOrderId());

        deliveryService.openDelivery(event);

        acknowledgment.acknowledge();
    }
}
