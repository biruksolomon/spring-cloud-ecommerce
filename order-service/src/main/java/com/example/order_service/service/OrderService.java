package com.example.order_service.service;


import com.example.order_service.client.ProductClient;
import com.example.order_service.domin.Order;
import com.example.order_service.dtos.ProductResponseDto;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
//    private final RestTemplate restTemp;
    private ProductClient productClient;
    public OrderService(
            OrderRepository orderRepository,
            RestTemplate restTemp) {
        this.orderRepository = orderRepository;
//        this.restTemp= restTemp;
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

        return orderRepository.save(order);
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
