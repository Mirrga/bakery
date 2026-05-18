package com.example.bakery.feature.order.repository;

import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // --- Стандартные методы с пагинацией ---

    /**
     * Поиск заказов пользователя с пагинацией.
     * Важно: используем JOIN FETCH, чтобы избежать N+1 при доступе к items в сервисе/контроллере.
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.user.username = :username ORDER BY o.createdAt DESC")
    Page<Order> findByUserUsername(@Param("username") String username, Pageable pageable);

    /**
     * Альтернатива без пагинации (если нужно просто список).
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.items JOIN FETCH o.user WHERE o.user.username = :username ORDER BY o.createdAt DESC")
    List<Order> findAllByUserUsername(@Param("username") String username);

    // --- Специфические JPQL запросы (Требование проекта) ---

    /**
     * Пример сложного JPQL запроса: выборка заказов по статусу с подгрузкой данных.
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByStatus(@Param("status") OrderStatus status);

    /**
     * Подсчет суммы заказов пользователя (пример агрегации).
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.username = :username")
    Optional<Double> calculateTotalSpentByUser(@Param("username") String username);
    
    /**
     * Поиск одного заказа по ID с полной загрузкой связей (для детального просмотра).
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.items JOIN FETCH o.user WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndUser(@Param("id") Long id);

    List<Order> findAllByUserId(Long userId, Sort sort);
}