package com.example.delivery_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A postal address. Embedded twice into {@link Delivery} - once as the
 * shipment's origin (product's warehouse) and once as its destination
 * (customer's delivery address) - via @AttributeOverrides on each field,
 * so both live as plain columns on the deliveries table rather than a
 * shared address table neither side has a real foreign-key relationship
 * to.
 */
@Embeddable
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Address {

    @Column(length = 120)
    private String recipientName;

    @Column(length = 32)
    private String phone;

    @Column(length = 200)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;
}
