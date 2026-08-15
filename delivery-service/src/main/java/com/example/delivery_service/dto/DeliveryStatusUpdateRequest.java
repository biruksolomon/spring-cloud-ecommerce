package com.example.delivery_service.dto;

import com.example.delivery_service.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Body for PUT /deliveries/{id}/status. Only {@code status} is required -
 * carrier/trackingNumber are typically set once (on the transition into
 * DISPATCHED) and location/note are free-text context for the tracking
 * history entry this update creates.
 */
@Getter
@Setter
public class DeliveryStatusUpdateRequest {

    @NotNull(message = "status is required")
    private DeliveryStatus status;

    private String carrier;

    private String trackingNumber;

    private String location;

    private String note;

    private Long estimatedDeliveryDate;
}
