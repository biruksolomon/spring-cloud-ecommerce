package com.example.order_service.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or a whole controller) as restricted to the
 * given role(s). Enforced by {@link RoleAuthorizationInterceptor}.
 * <p>
 * Endpoints left unannotated are open to any authenticated caller the
 * gateway lets through - createOrder and getOrder both stay owner-checked
 * in OrderService rather than role-gated, since any customer is allowed
 * to create/view their own orders. Only the admin-wide operations
 * (list all orders, force a status change) carry this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    Role[] value();
}