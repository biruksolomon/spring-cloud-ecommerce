package com.example.delivery_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single entry in a delivery's tracking history - "the shipment moved to
 * status X, at location Y, at time Z". Persisted as its own table (rather
 * than an @ElementCollection) so each entry keeps its own id and can be
 * queried/ordered independently of the parent Delivery.
 */
@Entity
@Table(name = "delivery_tracking_events")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    // Free-text location for this scan/update, e.g. "Addis Ababa sorting facility".
    private String location;

    // Optional human-readable note, e.g. "Recipient not available, retrying tomorrow".
    private String note;

    @Column(nullable = false, updatable = false)
    private Long occurredAt = System.currentTimeMillis();

    public DeliveryTrackingEvent(Delivery delivery, DeliveryStatus status, String location, String note) {
        this.delivery = delivery;
        this.status = status;
        this.location = location;
        this.note = note;
    }
}
