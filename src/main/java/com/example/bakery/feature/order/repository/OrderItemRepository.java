package com.example.bakery.feature.order.repository;

import com.example.bakery.feature.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Найти все позиции заказа (обычно доступно через order.getItems(), но иногда нужно отдельно).
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Пример JPQL: самые популярные товары (по количеству продаж).
     */
    @Query("SELECT oi.product, SUM(oi.quantity) as totalQty FROM OrderItem oi GROUP BY oi.product ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts(@Param("limit") int limit);
}