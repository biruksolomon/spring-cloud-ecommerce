package com.example.auth_service.security;

import com.example.auth_service.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Reads the X-User-Id / X-User-Role headers forwarded by the api-gateway's
 * JwtAuthFilter and exposes them as request attributes.
 * <p>
 * auth-service itself issues the tokens, but it still needs to know who is
 * calling GET /auth/user/{userId} and POST /auth/logout-all - those
 * endpoints act on/reveal another identity, so the caller must be checked.
 * See AuthController.getUser / AuthController.logoutAll.
 * <p>
 * Trust boundary: these headers are only honored if the request also
 * carries a valid X-Internal-Service-Token. Only api-gateway is configured
 * with that token, so a request that reaches this service directly (not
 * through the gateway) with a forged X-User-Id/X-User-Role pair is
 * rejected outright rather than silently trusted.
 */
@Slf4j
@Component
public class UserContextFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Value("${app.internal-service-token}")
    private String internalServiceToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean claimsIdentity = request.getHeader("X-User-Id") != null || request.getHeader("X-User-Role") != null;

        if (claimsIdentity && !hasValidInternalToken(request)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Identity headers require a valid internal service token");
            return;
        }

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

    private boolean hasValidInternalToken(HttpServletRequest request) {
        String supplied = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                internalServiceToken.getBytes(StandardCharsets.UTF_8));
    }
}