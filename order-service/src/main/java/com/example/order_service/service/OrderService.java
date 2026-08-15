package com.example.order_service.service;


import com.example.order_service.client.ProductClient;
import com.example.order_service.domin.DeliveryAddress;
import com.example.order_service.domin.Order;
import com.example.order_service.dto.AddressDto;
import com.example.order_service.dto.ProductResponseDto;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.event.OrderStatusEvent;
import com.example.order_service.exception.InvalidOrderStatusException;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.exception.UnauthorizedOrderAccessException;
import com.example.order_service.publisher.KafkaOrderPublisher;
import com.example.order_service.publisher.KafkaOrderStatusPublisher;
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
    // existing `status varchar` column. SHIPPED/DELIVERED are driven by
    // delivery-service via applyDeliveryResult rather than the admin
    // status endpoint, but still need to be valid values for that column.
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "CANCELLED", "SHIPPED", "DELIVERED");

    private final OrderRepository orderRepository;

    private final KafkaOrderPublisher kafkaOrderPublisher;
    private final KafkaOrderStatusPublisher kafkaOrderStatusPublisher;
//    private final OrderPublisher orderPublisher;
//    private final RestTemplate restTemp;

    private final ProductClient productClient;


    public OrderService(
            OrderRepository orderRepository, KafkaOrderPublisher kafkaOrderPublisher,
            KafkaOrderStatusPublisher kafkaOrderStatusPublisher
            /*RestTemplate restTemp,*/ /*OrderPublisher orderPublisher*/, ProductClient productClient) {
        this.orderRepository = orderRepository;
        //        this.restTemp= restTemp;
//        this.orderPublisher = orderPublisher;

        this.kafkaOrderPublisher = kafkaOrderPublisher;
        this.kafkaOrderStatusPublisher = kafkaOrderStatusPublisher;
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
                        savedorder.getTotalPrice(),
                        toAddressDto(savedorder.getDeliveryAddress()),
                        // Product's origin address as returned by product-service -
                        // already an AddressDto, forwarded as-is.
                        productResponseDto.getAddress()
                )
        );
        return savedorder;
    }

    private AddressDto toAddressDto(DeliveryAddress deliveryAddress) {
        if (deliveryAddress == null) {
            return null;
        }
        return new AddressDto(
                deliveryAddress.getRecipientName(),
                deliveryAddress.getPhone(),
                deliveryAddress.getStreet(),
                deliveryAddress.getCity(),
                deliveryAddress.getState(),
                deliveryAddress.getPostalCode(),
                deliveryAddress.getCountry()
        );
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
        Order saved = orderRepository.save(order);

        kafkaOrderStatusPublisher.publish(
                new OrderStatusEvent(saved.getOrderId(), saved.getUserId(), saved.getStatus(), saved.getUpdatedAt())
        );

        return saved;
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
        Order saved = orderRepository.save(order);

        if ("CANCELLED".equals(newStatus)) {
            try {
                productClient.restore(order.getProductId(), order.getQuantity());
            } catch (RuntimeException ex) {
                log.error("Failed to restore inventory for cancelled order {}", orderId, ex);
                throw ex;
            }
        }

        // Lets delivery-service know the order is either ready to be
        // processed into a shipment (CONFIRMED) or should have its
        // shipment cancelled (CANCELLED).
        kafkaOrderStatusPublisher.publish(
                new OrderStatusEvent(saved.getOrderId(), saved.getUserId(), saved.getStatus(), saved.getUpdatedAt())
        );

        log.info("Order {} moved to status {} following payment result {}", orderId, newStatus, paymentStatus);
    }

    // Applies a shipment-progress update from delivery-service to the
    // matching Order, so the order's own status reflects where its
    // package physically is. Mirrors applyPaymentResult's idempotency
    // shape, but keyed off delivery status rather than payment status and
    // without the "must currently be PENDING" guard, since a shipment can
    // legitimately move through several statuses (DISPATCHED, IN_TRANSIT,
    // DELIVERED) after an order is CONFIRMED.
    public void applyDeliveryResult(Long orderId, String deliveryStatus) {

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Received delivery event for unknown orderId {}", orderId);
            return;
        }

        String newStatus = switch (deliveryStatus) {
            case "DISPATCHED", "IN_TRANSIT", "OUT_FOR_DELIVERY" -> "SHIPPED";
            case "DELIVERED" -> "DELIVERED";
            default -> null; // PENDING/PROCESSING/FAILED/CANCELLED/RETURNED don't map onto an order status change
        };

        if (newStatus == null || newStatus.equals(order.getStatus())) {
            return;
        }

        // A terminal order status should never be walked backwards by a
        // late/redelivered delivery event.
        if ("CANCELLED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
            log.info("Order {} already in terminal status {}, ignoring delivery status {}",
                    orderId, order.getStatus(), deliveryStatus);
            return;
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(System.currentTimeMillis());
        orderRepository.save(order);

        log.info("Order {} moved to status {} following delivery status {}", orderId, newStatus, deliveryStatus);
    }

}