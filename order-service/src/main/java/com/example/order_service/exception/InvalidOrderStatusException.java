package com.example.order_service.exception;

import java.util.Set;

public class InvalidOrderStatusException extends RuntimeException {
    public InvalidOrderStatusException(String status, Set<String> allowed) {
        super("Invalid order status '" + status + "'. Allowed values: " + allowed);
    }
}