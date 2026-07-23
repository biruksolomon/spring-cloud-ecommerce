package com.example.notification_service.service;

import com.example.notification_service.domain.Notification;
import com.example.notification_service.domain.NotificationStatus;
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
}
