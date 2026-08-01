package com.example.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtProvider jwtProvider;

    // Endpoints that never require a token, regardless of method.
    private static final List<String> PUBLIC_ANY_METHOD = Arrays.asList(
            "/auth/register",
            "/auth/login"
    );

    // Endpoints that are public for reads only - mutating methods
    // (POST/PUT/PATCH/DELETE) still require a valid token.
    private static final List<String> PUBLIC_READ_ONLY = List.of(
            "/products"
    );

    // Endpoints where mutating methods require the ADMIN role, even though
    // the caller is authenticated. Reads are governed by PUBLIC_READ_ONLY /
    // the default "any authenticated user" rule above, not this list.
    private static final List<String> ADMIN_ONLY_WRITE = List.of(
            "/products"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // Allow public endpoints
        if (isPublicEndpoint(requestPath, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtProvider.validateToken(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid or expired token");
            return;
        }

        try {
            Long userId = jwtProvider.extractUserId(token);
            String email = jwtProvider.extractEmail(token);
            String role = jwtProvider.extractRole(token);

            if (isAdminOnlyMutation(requestPath, method) && !"ADMIN".equals(role)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("Admin role required for this operation");
                return;
            }

            // Add headers for downstream services - these travel over the
            // network with the proxied request, unlike a servlet attribute
            CustomRequestWrapper wrappedRequest = new CustomRequestWrapper(request);
            wrappedRequest.addHeader("X-User-Id", String.valueOf(userId));
            wrappedRequest.addHeader("X-User-Email", email);
            wrappedRequest.addHeader("X-User-Role", role);

            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            log.error("JWT validation error", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized");
        }
    }

    private boolean isPublicEndpoint(String path, String method) {
        for (String endpoint : PUBLIC_ANY_METHOD) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }

        if ("GET".equalsIgnoreCase(method)) {
            for (String endpoint : PUBLIC_READ_ONLY) {
                if (path.startsWith(endpoint)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isAdminOnlyMutation(String path, String method) {
        if ("GET".equalsIgnoreCase(method)) {
            return false;
        }
        for (String endpoint : ADMIN_ONLY_WRITE) {
            if (path.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }
}