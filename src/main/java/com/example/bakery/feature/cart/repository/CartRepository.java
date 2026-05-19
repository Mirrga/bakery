package com.example.bakery.feature.cart.repository;

import com.example.bakery.feature.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    // Стандартный метод (может вызвать LazyInitializationException при доступе к items вне транзакции
    // Поиск корзины по sessionId
    Optional<Cart> findBySessionId(String sessionId);

    // Поиск корзины по sessionId с предварительной загрузкой товаров (важно для merge)
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionIdWithItems(@Param("sessionId") String sessionId);

    // === НОВЫЙ МЕТОД: Поиск корзины по userId ===
    Optional<Cart> findByUserId(Long userId);
}