package com.example.order_service.service;


import com.example.order_service.client.ProductClient;
import com.example.order_service.domin.Order;
import com.example.order_service.dtos.ProductResponseDto;
import com.example.order_service.event.OrderCreatedEvent;
import com.example.order_service.publisher.OrderPublisher;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderPublisher orderPublisher;
//    private final RestTemplate restTemp;

    private final ProductClient productClient;


    public OrderService(
            OrderRepository orderRepository,
            /*RestTemplate restTemp,*/ OrderPublisher orderPublisher, ProductClient productClient) {
        this.orderRepository = orderRepository;
        //        this.restTemp= restTemp;
        this.orderPublisher = orderPublisher;
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

        orderPublisher.publish(
                new OrderCreatedEvent(
                        savedorder.getOrderId(),
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

}
