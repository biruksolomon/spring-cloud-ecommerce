package com.example.order_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the X-User-Id header forwarded by the api-gateway's JwtAuthFilter
 * and exposes it as a request attribute, since callers in this service
 * (e.g. OrderController) read the authenticated user via
 * request.getAttribute("userId") rather than the raw header.
 */
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

        filterChain.doFilter(request, response);
    }
}