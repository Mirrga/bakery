package com.example.bakery.feature.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bakery.feature.cart.entity.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findBySessionId(String sessionId);
}