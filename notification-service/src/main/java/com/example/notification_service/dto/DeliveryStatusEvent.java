package com.example.notification_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryStatusEvent {

    private Long deliveryId;

    private Long orderId;

    private Long userId;

    private String status;

    private String location;

    private Long updatedAt;
}
