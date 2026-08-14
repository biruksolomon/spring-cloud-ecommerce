package com.example.product_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Reads the X-User-Id / X-User-Email / X-User-Role headers forwarded by the
 * api-gateway's JwtAuthFilter and exposes them as request attributes, the
 * same pattern order-service's UserContextFilter uses.
 * <p>
 * This filter never rejects a request for missing user context by itself -
 * it just makes the caller's identity available when present. Whether an
 * endpoint requires a given role is decided by
 * {@link RoleAuthorizationInterceptor} based on the {@link RequireRole}
 * annotation. A request with no identity headers at all (e.g. an anonymous
 * GET that the gateway let through) simply proceeds with no user context set.
 * <p>
 * Trust boundary, two rules:
 *  1. /products/{id}/reserve and /restore always require a valid
 *     X-Internal-Service-Token, since these are pure service-to-service
 *     calls (order-service calling product-service directly, bypassing
 *     the gateway) that never carry X-User-* headers at all - this is
 *     the original check, unchanged.
 *  2. Any request carrying X-User-* headers (regardless of path) now also
 *     requires that same token. Previously only rule 1 existed, which left
 *     every other endpoint trusting X-User-Id/X-User-Role from any direct
 *     caller - a request reaching product-service without going through
 *     the gateway could simply set X-User-Role: ADMIN and be believed.
 */
@Slf4j
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTR = "userId";
    public static final String USER_EMAIL_ATTR = "userEmail";
    public static final String USER_ROLE_ATTR = "userRole";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Value("${app.internal-service-token}")
    private String internalServiceToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean claimsIdentity = request.getHeader("X-User-Id") != null
                || request.getHeader("X-User-Email") != null
                || request.getHeader("X-User-Role") != null;

        boolean requiresInternalToken = claimsIdentity || isInventoryMutation(request);

        if (requiresInternalToken && !hasValidInternalToken(request)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "This request requires a valid internal service token");
            return;
        }

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

    private boolean hasValidInternalToken(HttpServletRequest request) {
        String supplied = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                internalServiceToken.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isInventoryMutation(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "POST".equalsIgnoreCase(request.getMethod())
                && (path.matches(".*/products/\\d+/reserve") || path.matches(".*/products/\\d+/restore"));
    }
}