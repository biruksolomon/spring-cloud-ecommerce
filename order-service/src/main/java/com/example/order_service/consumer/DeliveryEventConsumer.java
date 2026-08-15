package com.example.order_service.consumer;

import com.example.order_service.event.DeliveryStatusEvent;
import com.example.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeliveryEventConsumer {

    private final OrderService orderService;

    public DeliveryEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(
            topics = "delivery-events",
            groupId = "order-delivery-group",
            containerFactory = "deliveryEventListenerFactory"
    )
    public void receive(DeliveryStatusEvent event, Acknowledgment acknowledgment) {

        log.info("Order service received delivery event for orderId {}: {}",
                event.getOrderId(), event.getStatus());

        orderService.applyDeliveryResult(event.getOrderId(), event.getStatus());

        acknowledgment.acknowledge();
    }
}
