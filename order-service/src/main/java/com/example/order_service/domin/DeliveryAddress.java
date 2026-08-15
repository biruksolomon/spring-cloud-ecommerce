package com.example.order_service.domin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The destination address an order should be shipped to. Embedded into
 * {@link Order} (own columns via @Embedded) rather than a separate table,
 * since it has no lifecycle independent of the order it belongs to.
 * <p>
 * Carried on the OrderCreatedEvent so delivery-service can open a shipment
 * without calling back into order-service for the address.
 */
@Embeddable
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DeliveryAddress {

    @Column(name = "delivery_recipient_name")
    @NotBlank(message = "deliveryAddress.recipientName is required")
    private String recipientName;

    @Column(name = "delivery_phone")
    @NotBlank(message = "deliveryAddress.phone is required")
    private String phone;

    @Column(name = "delivery_street")
    @NotBlank(message = "deliveryAddress.street is required")
    private String street;

    @Column(name = "delivery_city")
    @NotBlank(message = "deliveryAddress.city is required")
    private String city;

    @Column(name = "delivery_state")
    private String state;

    @Column(name = "delivery_postal_code")
    private String postalCode;

    @Column(name = "delivery_country")
    @NotBlank(message = "deliveryAddress.country is required")
    private String country;
}
