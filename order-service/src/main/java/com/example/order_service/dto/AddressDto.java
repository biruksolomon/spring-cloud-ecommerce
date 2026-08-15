package com.example.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic street-address shape used both for the product's origin address
 * (deserialized from ProductClient's /products/{id} response) and for an
 * order's destination address when building the OrderCreatedEvent that
 * delivery-service consumes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

    private String recipientName;

    private String phone;

    private String street;

    private String city;

    private String state;

    private String postalCode;

    private String country;
}
