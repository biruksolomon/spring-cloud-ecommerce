package com.example.notification_service.service;

import com.example.notification_service.domain.Notification;
import com.example.notification_service.domain.NotificationStatus;
import com.example.notification_service.dto.DeliveryStatusEvent;
import com.example.notification_service.dto.OrderCreatedEvent;
import com.example.notification_service.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void saveNotification(OrderCreatedEvent event){
        Notification notification = new Notification();
        notification.setOrderId(event.getOrderId());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMessage("Order Created Successfully");
//        throw new RuntimeException("Database simulated failure");

        notificationRepository.save(notification);
    }

    // Turns a shipment status change into a human-readable notification
    // for the customer, mirroring saveNotification's shape for order
    // creation. Kept as its own method (rather than overloading
    // saveNotification) since the two events have unrelated fields.
    public void saveDeliveryNotification(DeliveryStatusEvent event) {
        Notification notification = new Notification();
        notification.setOrderId(event.getOrderId());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMessage(deliveryMessage(event));

        notificationRepository.save(notification);
    }

    private String deliveryMessage(DeliveryStatusEvent event) {
        String base = switch (event.getStatus()) {
            case "PROCESSING" -> "Your order is being prepared for shipment";
            case "DISPATCHED" -> "Your order has been dispatched to a carrier";
            case "IN_TRANSIT" -> "Your order is in transit";
            case "OUT_FOR_DELIVERY" -> "Your order is out for delivery";
            case "DELIVERED" -> "Your order has been delivered";
            case "FAILED" -> "A delivery attempt for your order failed";
            case "RETURNED" -> "Your order is being returned to the sender";
            case "CANCELLED" -> "The delivery for your order was cancelled";
            default -> "Your order's delivery status changed to " + event.getStatus();
        };
        return event.getLocation() != null ? base + " (" + event.getLocation() + ")" : base;
    }
}