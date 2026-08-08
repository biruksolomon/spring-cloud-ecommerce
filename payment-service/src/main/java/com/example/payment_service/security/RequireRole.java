package com.example.payment_service.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or a whole controller) as restricted to the
 * given role(s). Enforced by {@link RoleAuthorizationInterceptor}.
 * <p>
 * getPaymentByOrderId and getMyPayments stay unannotated - they're already
 * ownership-checked in PaymentService against the caller's own userId.
 * Only the admin-wide listing carries this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    Role[] value();
}