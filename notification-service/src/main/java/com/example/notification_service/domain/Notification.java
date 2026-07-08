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

    private String status;

    public Notification(Long orderId, String message, String status) {
        this.orderId = orderId;
        this.message = message;
        this.status = status;
    }

    public Notification() {

    }
}