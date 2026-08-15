package com.example.notification_service.consumer;

import com.example.notification_service.dto.DeliveryStatusEvent;
import com.example.notification_service.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeliveryConsumer {

    private final NotificationService notificationService;

    public DeliveryConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "delivery-events",
            groupId = "notification-delivery-group",
            containerFactory = "deliveryKafkaListenerContainerFactory"
    )
    public void receive(DeliveryStatusEvent event, Acknowledgment acknowledgment) {

        log.info("Notification service received delivery event for orderId {}: {}",
                event.getOrderId(), event.getStatus());

        notificationService.saveDeliveryNotification(event);

        acknowledgment.acknowledge();
    }
}
