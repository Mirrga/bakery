package com.example.bakery.feature.order.repository;

import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByUser(User user, Pageable pageable);

    // Пример JPQL запроса (требование проекта)
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByStatus(@Param("status") com.example.bakery.feature.order.entity.OrderStatus status);
}