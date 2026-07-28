package com.example.order_service.controller;


import com.example.order_service.domin.Order;
import com.example.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order, HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            order.setUserId(userId);
        }
        return orderService.createOrder(order);
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return orderService.getOrderById(orderId, userId);
    }
}
