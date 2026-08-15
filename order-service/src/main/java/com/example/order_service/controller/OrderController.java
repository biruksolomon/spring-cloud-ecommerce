package com.example.order_service.controller;


import com.example.order_service.domin.Order;
import com.example.order_service.dto.OrderStatusUpdateRequest;
import com.example.order_service.security.RequireRole;
import com.example.order_service.security.Role;
import com.example.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Any authenticated customer can create their own order - not role-gated,
    // ownership is set from the token (X-User-Id), not the request body.
    @PostMapping
    public Order createOrder(@Valid @RequestBody Order order, HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            order.setUserId(userId);
        }
        return orderService.createOrder(order);
    }

    // Any authenticated customer can look up their own order - ownership is
    // enforced in OrderService.getOrderById, not by a role check here.
    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return orderService.getOrderById(orderId, userId);
    }

    // Admin-only: browse every order in the system, not just the caller's own.
    @RequireRole(Role.ADMIN)
    @GetMapping("/all")
    public Page<Order> getAllOrders(
            @RequestParam int page,
            @RequestParam int size) {
        return orderService.getAllOrders(page, size);
    }

    // Admin-only: force any order's status (e.g. manually cancel a stuck
    // PENDING order), bypassing the normal payment-driven saga.
    @RequireRole(Role.ADMIN)
    @PutMapping("/{orderId}/status")
    public Order updateOrderStatus(@PathVariable Long orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateOrderStatus(orderId, request.getStatus());
    }
}