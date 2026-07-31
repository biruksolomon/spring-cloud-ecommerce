package com.example.order_service.service;


import com.example.order_service.client.ProductClient;
import com.example.order_service.domin.Order;
import com.example.order_service.dtos.ProductResponseDto;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.publisher.KafkaOrderPublisher;
import com.example.order_service.publisher.OrderPublisher;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderService {

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

       /* ProductResponseDto productResponseDto = restTemp.getForObject(
                "http://localhost:8081/products/"+order.getProductId(),ProductResponseDto.class
        );*/

        ProductResponseDto productResponseDto = productClient.getProduct(order.getProductId());
//        order.builder().totalPrice(productResponseDto.getPrice()* order.getQuantity());
        assert productResponseDto != null;
        order.setTotalPrice(productResponseDto.getPrice()* order.getQuantity());

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
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to this order");
        }

        return order;
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

        log.info("Order {} moved to status {} following payment result {}", orderId, newStatus, paymentStatus);
    }

}