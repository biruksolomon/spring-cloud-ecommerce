package com.example.product_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The warehouse / origin location a product ships from. Embedded directly
 * into {@link Product} (own table, own columns via @Embedded) rather than
 * a separate entity, since an address here has no independent lifecycle -
 * it only ever exists as part of a product.
 * <p>
 * This is what order-service pulls (via ProductClient) into an
 * OrderCreatedEvent so delivery-service can compute a shipment's origin
 * without a second network hop back to product-service.
 */
@Embeddable
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ProductAddress {

    @Column(name = "address_street")
    private String street;

    @Column(name = "address_city")
    private String city;

    @Column(name = "address_state")
    private String state;

    @Column(name = "address_postal_code")
    private String postalCode;

    @Column(name = "address_country")
    private String country;

    public boolean isEmpty() {
        return street == null && city == null && state == null && postalCode == null && country == null;
    }
}
