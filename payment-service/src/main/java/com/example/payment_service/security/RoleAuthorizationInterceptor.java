package com.example.payment_service.security;

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
 * Same defense-in-depth pattern as product-service and order-service:
 * reads {@link RequireRole} off the handler and checks it against the
 * "userRole" request attribute UserContextFilter sets from X-User-Role.
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