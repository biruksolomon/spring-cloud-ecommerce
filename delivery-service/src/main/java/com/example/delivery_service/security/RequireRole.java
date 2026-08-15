package com.example.delivery_service.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or a whole controller) as restricted to the
 * given role(s). Enforced by {@link RoleAuthorizationInterceptor}.
 * <p>
 * Logistics operations (assigning a carrier, advancing a shipment's
 * status) are ADMIN-only. Customers can only read their own delivery -
 * ownership is checked in DeliveryService, not via this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    Role[] value();
}
