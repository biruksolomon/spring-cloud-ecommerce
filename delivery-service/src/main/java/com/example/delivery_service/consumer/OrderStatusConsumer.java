package com.example.delivery_service.consumer;

import com.example.delivery_service.event.OrderStatusEvent;
import com.example.delivery_service.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderStatusConsumer {

    private final DeliveryService deliveryService;

    public OrderStatusConsumer(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = "order-status-events",
            groupId = "delivery-order-status-group",
            containerFactory = "orderStatusListenerFactory"
    )
    public void receive(OrderStatusEvent event, Acknowledgment acknowledgment) {
        log.info("Delivery service received order-status event for orderId {}: {}",
                event.getOrderId(), event.getStatus());

        deliveryService.applyOrderStatus(event.getOrderId(), event.getStatus());

        acknowledgment.acknowledge();
    }
}
