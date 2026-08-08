package com.example.product_service.security;

/**
 * Mirrors auth-service's Role enum. Kept as a separate local type
 * (rather than a shared dependency) since each service should be
 * independently deployable and shouldn't compile against another
 * service's internal classes. The string value coming from the
 * X-User-Role header must match one of these names.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}