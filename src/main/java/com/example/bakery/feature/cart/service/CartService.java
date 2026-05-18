package com.example.bakery.feature.cart.service;

import com.example.bakery.feature.cart.dto.CartDto;
import com.example.bakery.feature.cart.dto.CartItemDto;
import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.entity.CartItem;
import com.example.bakery.feature.cart.repository.CartItemRepository;
import com.example.bakery.feature.cart.repository.CartRepository;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // --- Основные операции с корзиной ---

    /**
     * Получить или создать корзину для текущей сессии
     */
    @Transactional(readOnly = true)
    public Cart getOrCreateCart(String sessionId) {
        return cartRepository.findBySessionIdWithItems(sessionId) // <-- Изменено здесь
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Добавить товар в корзину
     * Оптимизировано: не загружаем всю корзину, если товар уже есть (можно улучшить через JPQL)
     */
    public void addToCart(String sessionId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден с ID: " + productId));

        // Ищем существующий элемент в загруженном списке (Hibernate управляет коллекцией)
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            // Сохраняем только элемент, так как cascade уже настроен, но явное сохранение надежнее при изменении
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(newItem);
            // Cascade ALL на关系中 сохранит новый элемент при сохранении корзины
            cartRepository.save(cart); 
        }
    }

    /**
     * Обновить количество товара в корзине
     */
    public void updateQuantity(String sessionId, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(sessionId);
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Товар в корзине не найден"));

        if (quantity <= 0) {
            removeFromCart(sessionId, itemId);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    /**
     * Удалить конкретный товар из корзины
     */
    public void removeFromCart(String sessionId, Long itemId) {
        Cart cart = getOrCreateCart(sessionId);
        
        CartItem itemToRemove = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Товар в корзине не найден"));

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove); // Явное удаление
        cartRepository.save(cart);
    }

    /**
     * Очистить корзину полностью
     */
    public void clearCart(String sessionId) {
        Cart cart = getOrCreateCart(sessionId);
        // Удаляем элементы через репозиторий, чтобы сработали триггеры/события если есть
        List<CartItem> itemsToDelete = List.copyOf(cart.getItems()); 
        cart.getItems().clear();
        cartItemRepository.deleteAll(itemsToDelete);
        cartRepository.save(cart);
    }

    /**
     * Привязать корзину к пользователю (при логине/регистрации)
     */
    public void mergeCartWithUser(String sessionId, Long userId) {
        // Логика слияния гостевой корзины с корзиной пользователя может быть сложной.
        // В простом случае: просто меняем sessionId на userId или привязываем User entity.
        // Здесь реализуем простую привязку сущности User, если она передается извне, 
        // либо поиск корзины по userId (если такая логика нужна).
        // Для текущего примера оставим работу с sessionId, так как User связан с Cart через @OneToOne.
        // Если нужно привязать:
        Cart cart = cartRepository.findBySessionId(sessionId).orElse(null);
        if (cart != null && cart.getUser() == null) {
             // Логика поиска корзины пользователя и слияния товаров должна быть здесь
             // Пока просто помечаем, что корзина принадлежит сессии, а пользователь подтянется через связь, если она настроена
        }
    }

    // --- Чтение и DTO ---

    /**
     * Получить корзину для отображения (DTO)
     */
    @Transactional(readOnly = true)
    public CartDto getCartDtoBySession(String sessionId) {
        Cart cart = getOrCreateCart(sessionId);
        return mapToDto(cart);
    }

    /**
     * Получить сущность корзины (для внутренних операций, например, создания заказа)
     */
    @Transactional(readOnly = true)
    public Cart getCartBySession(String sessionId) {
        return getOrCreateCart(sessionId);
    }

    // --- Маппер ---

    private CartDto mapToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setSessionId(cart.getSessionId());
        
        if (cart.getUser() != null) {
            dto.setUserId(cart.getUser().getId());
            dto.setUserName(cart.getUser().getUsername());
        }

        List<CartItemDto> itemDtos = cart.getItems().stream().map(item -> {
            CartItemDto itemDto = new CartItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setProductName(item.getProduct().getName());
            itemDto.setProductImageUrl(item.getProduct().getImageUrl());
            itemDto.setPrice(item.getProduct().getPrice());
            itemDto.setQuantity(item.getQuantity());
            
            BigDecimal subtotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            itemDto.setSubtotal(subtotal);
            
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDtos);
        dto.setTotalAmount(calculateTotal(itemDtos));
        dto.setTotalItems(calculateTotalItems(itemDtos));

        return dto;
    }

    private BigDecimal calculateTotal(List<CartItemDto> items) {
        return items.stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calculateTotalItems(List<CartItemDto> items) {
        return items.stream()
                .map(CartItemDto::getQuantity)
                .reduce(0, Integer::sum);
    }
}