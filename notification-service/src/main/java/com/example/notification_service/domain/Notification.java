package com.example.notification_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    public Notification(Long orderId, String message, NotificationStatus status) {
        this.orderId = orderId;
        this.message = message;
        this.status = status;
    }

    public Notification() {

    }
}