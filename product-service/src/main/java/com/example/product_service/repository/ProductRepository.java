package com.example.product_service.repository;

import com.example.product_service.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("update Product p set p.quantity = p.quantity - :quantity "
            + "where p.productId = :productId and p.quantity >= :quantity")
    int reserve(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("update Product p set p.quantity = p.quantity + :quantity "
            + "where p.productId = :productId")
    int restore(@Param("productId") Long productId, @Param("quantity") int quantity);
}
