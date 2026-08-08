package com.example.product_service.security;

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
 * the controller class) and checks it against the role
 * {@link UserContextFilter} attached to the request.
 * <p>
 * This is the defense-in-depth layer: the api-gateway already blocks
 * non-admin writes to /products, but product-service should not rely
 * solely on that - anything reachable on the internal network (another
 * service, a direct call during local dev, a misconfigured route) should
 * still be turned away here if it isn't an admin.
 */
@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

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
            // Not annotated - endpoint is open to anyone the gateway let through.
            return true;
        }

        Object roleAttr = request.getAttribute(UserContextFilter.USER_ROLE_ATTR);

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