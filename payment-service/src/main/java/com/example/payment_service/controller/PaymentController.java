package com.example.payment_service.controller;

import com.example.payment_service.domain.Payment;
import com.example.payment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Look up the payment for a specific order. Payments are created async
    // (by the order-events consumer), so a fresh order may briefly 404 here
    // until payment-service has processed it.
    @GetMapping("/{orderId}")
    public Payment getPaymentByOrderId(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return paymentService.getPaymentByOrderId(orderId, userId);
    }

    @GetMapping
    public List<Payment> getMyPayments(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return paymentService.getPaymentsForUser(userId);
    }
}