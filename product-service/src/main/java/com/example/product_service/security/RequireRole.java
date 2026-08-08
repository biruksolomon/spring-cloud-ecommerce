package com.example.product_service.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or a whole controller) as restricted to the
 * given role(s). Enforced by {@link RoleAuthorizationInterceptor}.
 * <p>
 * Any endpoint left unannotated is open to all callers the gateway lets
 * through - this is how product reads stay public while writes lock down
 * to ADMIN, without a growing allow/deny list to keep in sync by hand as
 * more admin-only features are added.
 * <p>
 * Example:
 * <pre>
 *   {@literal @}RequireRole(Role.ADMIN)
 *   {@literal @}PostMapping
 *   public Product create(...) { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireRole {
    Role[] value();
}