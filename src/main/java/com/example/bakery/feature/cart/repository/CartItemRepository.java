package com.example.bakery.feature.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bakery.feature.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}