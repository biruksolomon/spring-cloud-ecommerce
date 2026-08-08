package com.example.order_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the X-User-Id / X-User-Email / X-User-Role headers forwarded by the
 * api-gateway's JwtAuthFilter and exposes them as request attributes, since
 * callers in this service (e.g. OrderController) read the authenticated
 * user via request.getAttribute(...) rather than the raw headers.
 * <p>
 * The role attribute ("userRole") is what RoleAuthorizationInterceptor
 * checks against @RequireRole on admin-only endpoints.
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            try {
                request.setAttribute("userId", Long.valueOf(userIdHeader));
            } catch (NumberFormatException ignored) {
                // malformed header - leave userId unset rather than fail the request
            }
        }

        String email = request.getHeader("X-User-Email");
        if (email != null) {
            request.setAttribute("email", email);
        }

        String roleHeader = request.getHeader("X-User-Role");
        if (roleHeader != null) {
            try {
                request.setAttribute("userRole", Role.valueOf(roleHeader));
            } catch (IllegalArgumentException e) {
                log.warn("Unrecognized X-User-Role header value: {}", roleHeader);
            }
        }

        filterChain.doFilter(request, response);
    }
}