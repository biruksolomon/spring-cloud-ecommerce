package com.example.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {

    private Long orderId;

    private Long paymentId;

    private String status; // "SUCCESS" or "FAILED"
}