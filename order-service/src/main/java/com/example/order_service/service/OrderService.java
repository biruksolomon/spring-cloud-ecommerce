package com.example.order_service.service;


import com.example.order_service.client.ProductClient;
import com.example.order_service.domin.Order;
import com.example.order_service.dto.ProductResponseDto;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.exception.InvalidOrderStatusException;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.exception.UnauthorizedOrderAccessException;
import com.example.order_service.publisher.KafkaOrderPublisher;
import com.example.order_service.publisher.OrderPublisher;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class OrderService {

    // The statuses the saga (create -> pay -> confirm/cancel) and admin
    // overrides are allowed to move an order into. Kept as a fixed set
    // rather than a JPA enum so this doesn't require a migration on the
    // existing `status varchar` column.
    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "CONFIRMED", "CANCELLED");

    private final OrderRepository orderRepository;

    private final KafkaOrderPublisher kafkaOrderPublisher;
//    private final OrderPublisher orderPublisher;
//    private final RestTemplate restTemp;

    private final ProductClient productClient;


    public OrderService(
            OrderRepository orderRepository,KafkaOrderPublisher kafkaOrderPublisher
            /*RestTemplate restTemp,*/ /*OrderPublisher orderPublisher*/, ProductClient productClient) {
        this.orderRepository = orderRepository;
        //        this.restTemp= restTemp;
//        this.orderPublisher = orderPublisher;

        this.kafkaOrderPublisher = kafkaOrderPublisher;
        this.productClient = productClient;
    }

    @CircuitBreaker(
            name = "productService",
            fallbackMethod = "productFallback"
    )
    public Order createOrder(Order order){
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

       /* ProductResponseDto productResponseDto = restTemp.getForObject(
                "http://localhost:8081/products/"+order.getProductId(),ProductResponseDto.class
        );*/

        ProductResponseDto productResponseDto = productClient.getProduct(order.getProductId());
//        order.builder().totalPrice(productResponseDto.getPrice()* order.getQuantity());
        if (productResponseDto == null || productResponseDto.getPrice() == null) {
            throw new IllegalStateException("Product details are unavailable");
        }
        productClient.reserve(order.getProductId(), order.getQuantity());
        order.setTotalPrice(productResponseDto.getPrice() * order.getQuantity());
        order.setStatus("PENDING");

        Order savedorder = orderRepository.save(order);

      /*  orderPublisher.publish(
                new OrderCreatedEvent(
                        savedorder.getOrderId(),
                        savedorder.getProductId(),
                        savedorder.getQuantity(),
                        savedorder.getTotalPrice()
                )
        );*/
        kafkaOrderPublisher.publish(
                new OrderCreatedEvent(
                        savedorder.getOrderId(),
                        savedorder.getUserId(),
                        savedorder.getProductId(),
                        savedorder.getQuantity(),
                        savedorder.getTotalPrice()
                )
        );
        return savedorder;
    }

    public Order productFallback(
            Order order,
            Exception ex
    ){
        throw new RuntimeException(
                "Product Service is unavailable. Please try again later."
        );
    }

    public Order getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedOrderAccessException(orderId);
        }

        return order;
    }

    // Admin-only: list every order regardless of owner. Guarded by
    // @RequireRole(ADMIN) on the controller method, not by any check here -
    // this method itself has no notion of "whose" orders it's returning.
    public Page<Order> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable);
    }

    // Admin-only: force an order's status regardless of the normal
    // create -> pay -> confirm/cancel saga. Useful for support overrides
    // (e.g. manually cancelling a stuck PENDING order). Guarded by
    // @RequireRole(ADMIN) on the controller method.
    public Order updateOrderStatus(Long orderId, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new InvalidOrderStatusException(newStatus, VALID_STATUSES);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.setStatus(newStatus);
        order.setUpdatedAt(System.currentTimeMillis());
        return orderRepository.save(order);
    }

    // Applies the outcome of a PaymentEvent to the matching Order, closing
    // the create-order -> charge-payment -> confirm-or-cancel saga.
    public void applyPaymentResult(Long orderId, String paymentStatus) {

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            // Order not found (shouldn't happen in practice since orderId
            // comes from an order this service created) - log and stop
            // rather than throw, since throwing here would just cause an
            // endless redelivery loop for a message that can never succeed.
            log.warn("Received payment result for unknown orderId {}", orderId);
            return;
        }

        // Idempotency guard: once an order has left PENDING, a redelivered
        // or duplicate payment-events message should not flip it again.
        if (!"PENDING".equals(order.getStatus())) {
            log.info("Order {} already in status {}, ignoring payment result {}",
                    orderId, order.getStatus(), paymentStatus);
            return;
        }

        String newStatus = "SUCCESS".equals(paymentStatus) ? "CONFIRMED" : "CANCELLED";

        order.setStatus(newStatus);
        order.setUpdatedAt(System.currentTimeMillis());
        orderRepository.save(order);

        if ("CANCELLED".equals(newStatus)) {
            try {
                productClient.restore(order.getProductId(), order.getQuantity());
            } catch (RuntimeException ex) {
                log.error("Failed to restore inventory for cancelled order {}", orderId, ex);
                throw ex;
            }
        }

        log.info("Order {} moved to status {} following payment result {}", orderId, newStatus, paymentStatus);
    }

}