package com.example.bakery.feature.cart.service;

import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.entity.CartItem;
import com.example.bakery.feature.cart.repository.CartItemRepository;
import com.example.bakery.feature.cart.repository.CartRepository;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // Получить или создать корзину для текущей сессии
    public Cart getOrCreateCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
    }

    // Добавить товар в корзину
    public void addToCart(String sessionId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        // Проверяем, есть ли уже такой товар в корзине
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Если есть — увеличиваем количество
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Если нет — создаем новую позицию
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(newItem);
            cartRepository.save(cart); // Cascade save сохранит и CartItem
        }
    }
    
    // Получить корзину с товарами (для отображения)
    public Cart getCartBySession(String sessionId) {
        return getOrCreateCart(sessionId);
    }
}