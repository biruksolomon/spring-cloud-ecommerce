package com.example.delivery_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
