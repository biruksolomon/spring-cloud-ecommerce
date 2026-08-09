package com.example.payment_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "orderId"))
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // Stripe's id for the PaymentIntent backing this payment, e.g.
    // "pi_3Nxxx...". Nullable because a row created before Stripe
    // integration existed, or one that failed before Stripe was even
    // called, won't have one. Used to match incoming webhook events
    // back to this row.
    @Column(unique = true)
    private String stripePaymentIntentId;

    @Column(nullable = false, updatable = false)
    private Long createdAt = System.currentTimeMillis();
}