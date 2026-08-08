package com.example.product_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the X-User-Id / X-User-Email / X-User-Role headers forwarded by the
 * api-gateway's JwtAuthFilter and exposes them as request attributes, the
 * same pattern order-service's UserContextFilter uses.
 * <p>
 * This filter never rejects a request by itself - it just makes the caller's
 * identity available. Whether an endpoint requires a given role is decided
 * by {@link RoleAuthorizationInterceptor} based on the {@link RequireRole}
 * annotation. A request with no headers at all (e.g. an anonymous GET that
 * the gateway let through) simply proceeds with no user context set.
 */
@Slf4j
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTR = "userId";
    public static final String USER_EMAIL_ATTR = "userEmail";
    public static final String USER_ROLE_ATTR = "userRole";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            try {
                request.setAttribute(USER_ID_ATTR, Long.valueOf(userIdHeader));
            } catch (NumberFormatException ignored) {
                // malformed header - leave userId unset rather than fail the request
            }
        }

        String email = request.getHeader("X-User-Email");
        if (email != null) {
            request.setAttribute(USER_EMAIL_ATTR, email);
        }

        String roleHeader = request.getHeader("X-User-Role");
        if (roleHeader != null) {
            try {
                request.setAttribute(USER_ROLE_ATTR, Role.valueOf(roleHeader));
            } catch (IllegalArgumentException e) {
                log.warn("Unrecognized X-User-Role header value: {}", roleHeader);
            }
        }

        filterChain.doFilter(request, response);
    }
}