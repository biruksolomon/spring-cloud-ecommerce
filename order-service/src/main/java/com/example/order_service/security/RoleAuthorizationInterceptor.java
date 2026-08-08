package com.example.order_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads {@link RequireRole} off the handler (method first, falling back to
 * the controller class) and checks it against the role that
 * UserContextFilter attached to the request as "userRole".
 * <p>
 * Same defense-in-depth reasoning as product-service: the gateway already
 * requires a valid token for every non-public /orders endpoint, but it
 * doesn't know which of those are admin-only - that decision lives here,
 * next to the endpoints it protects.
 */
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    public static final String USER_ROLE_ATTR = "userRole";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        if (requireRole == null) {
            return true;
        }

        Object roleAttr = request.getAttribute(USER_ROLE_ATTR);

        if (roleAttr == null) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing or unrecognized caller role");
            return false;
        }

        Set<Role> allowed = Arrays.stream(requireRole.value()).collect(Collectors.toSet());
        if (!allowed.contains(roleAttr)) {
            reject(response, HttpStatus.FORBIDDEN, "This operation requires role " + allowed + ", caller has " + roleAttr);
            return false;
        }

        return true;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
    }
}