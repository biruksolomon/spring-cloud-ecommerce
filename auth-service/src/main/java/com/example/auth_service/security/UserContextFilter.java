package com.example.auth_service.security;

import com.example.auth_service.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the X-User-Id / X-User-Role headers forwarded by the api-gateway's
 * JwtAuthFilter and exposes them as request attributes.
 * <p>
 * auth-service itself issues the tokens, but it still needs to know who is
 * calling GET /auth/user/{userId} - that endpoint returns another user's
 * profile, so it has to check the caller is either that same user or an
 * admin. See AuthController.getUser.
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