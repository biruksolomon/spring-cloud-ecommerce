package com.example.delivery_service.controller;

import com.example.delivery_service.domain.Delivery;
import com.example.delivery_service.dto.DeliveryStatusUpdateRequest;
import com.example.delivery_service.security.RequireRole;
import com.example.delivery_service.security.Role;
import com.example.delivery_service.service.DeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // A customer can view their own shipment; an admin can view any.
    // Ownership is enforced in DeliveryService, not by a role check here.
    @GetMapping("/{id}")
    public Delivery getById(@PathVariable Long id, HttpServletRequest request) {
        return deliveryService.getById(id, callerUserId(request), isAdmin(request));
    }

    // Same ownership rule as above, but looked up by orderId - the natural
    // key a customer or order-service actually has on hand.
    @GetMapping("/order/{orderId}")
    public Delivery getByOrderId(@PathVariable Long orderId, HttpServletRequest request) {
        return deliveryService.getByOrderId(orderId, callerUserId(request), isAdmin(request));
    }

    // Admin/logistics-only: browse every shipment in the system.
    @RequireRole(Role.ADMIN)
    @GetMapping
    public Page<Delivery> getAll(@RequestParam int page, @RequestParam int size) {
        return deliveryService.getAll(page, size);
    }

    // Admin/logistics-only: advance a shipment's status, optionally
    // assigning a carrier/tracking number and appending a tracking-history
    // entry with a location/note.
    @RequireRole(Role.ADMIN)
    @PutMapping("/{id}/status")
    public Delivery updateStatus(@PathVariable Long id, @Valid @RequestBody DeliveryStatusUpdateRequest request) {
        return deliveryService.updateStatus(id, request);
    }

    private Long callerUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private boolean isAdmin(HttpServletRequest request) {
        return Role.ADMIN.equals(request.getAttribute("userRole"));
    }
}
