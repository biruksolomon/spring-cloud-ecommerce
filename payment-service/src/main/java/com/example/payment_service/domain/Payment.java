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
    // called, won't have one. Populated once the Checkout Session
    // resolves to a PaymentIntent (either via webhook or the session
    // lookup fallback), used to match incoming webhook events back to
    // this row.
    @Column(unique = true)
    private String stripePaymentIntentId;

    // Stripe Checkout Session id, e.g. "cs_test_...". Set as soon as the
    // session is created, before the PaymentIntent id is known - this is
    // what lets the webhook find this row when a
    // checkout.session.completed event arrives.
    @Column(unique = true)
    private String stripeCheckoutSessionId;

    // The hosted Stripe Checkout URL the customer needs to open to pay.
    // This is what callers of GET /payments/{orderId} use to redirect
    // the customer, instead of this service silently charging a card on
    // its own. Null once the session has expired/been consumed, but we
    // keep it as a record of the last URL we handed out.
    @Column(length = 2048)
    private String checkoutUrl;

    @Column(nullable = false, updatable = false)
    private Long createdAt = System.currentTimeMillis();
}