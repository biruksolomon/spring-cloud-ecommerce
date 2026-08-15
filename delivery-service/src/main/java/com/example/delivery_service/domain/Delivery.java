package com.example.delivery_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A shipment for a single order. Created (status PENDING) as soon as
 * order-service publishes an OrderCreatedEvent, then driven forward
 * through the DeliveryStatus lifecycle either by order-status-events
 * (PROCESSING on payment confirmation, CANCELLED on cancellation) or by
 * logistics staff via the REST API (DISPATCHED onward).
 */
@Entity
@Table(name = "deliveries")
@Getter
@Setter
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    // One delivery per order - the join point back to order-service.
    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    // Where the shipment ships from - the product's warehouse address at
    // the time the order was placed.
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "recipientName", column = @Column(name = "origin_recipient_name")),
            @AttributeOverride(name = "phone", column = @Column(name = "origin_phone")),
            @AttributeOverride(name = "street", column = @Column(name = "origin_street")),
            @AttributeOverride(name = "city", column = @Column(name = "origin_city")),
            @AttributeOverride(name = "state", column = @Column(name = "origin_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "origin_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "origin_country"))
    })
    private Address origin = new Address();

    // Where the shipment is going - the customer's delivery address.
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "recipientName", column = @Column(name = "destination_recipient_name")),
            @AttributeOverride(name = "phone", column = @Column(name = "destination_phone")),
            @AttributeOverride(name = "street", column = @Column(name = "destination_street")),
            @AttributeOverride(name = "city", column = @Column(name = "destination_city")),
            @AttributeOverride(name = "state", column = @Column(name = "destination_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "destination_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "destination_country"))
    })
    private Address destination = new Address();

    // Set by logistics staff once the shipment is handed to a carrier
    // (status DISPATCHED). Null before that.
    private String carrier;

    private String trackingNumber;

    // Epoch millis, matching the style of timestamps elsewhere in this
    // system (Order.createdAt, Notification has none but events use Long).
    private Long estimatedDeliveryDate;

    private Long actualDeliveryDate;

    @Column(nullable = false, updatable = false)
    private Long createdAt = System.currentTimeMillis();

    private Long updatedAt;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<DeliveryTrackingEvent> trackingHistory = new ArrayList<>();

    public void addTrackingEvent(DeliveryStatus status, String location, String note) {
        trackingHistory.add(new DeliveryTrackingEvent(this, status, location, note));
    }
}
