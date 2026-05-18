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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // --- Основные операции с корзиной ---

    /**
     * Получить или создать корзину для текущей сессии
     */
    @Transactional(readOnly = true)
    public Cart getOrCreateCart(String sessionId) {
        log.debug("Поиск корзины для сессии: {}", sessionId);
        return cartRepository.findBySessionIdWithItems(sessionId)
                .orElseGet(() -> {
                    log.info("Корзина не найдена, создаем новую для сессии: {}", sessionId);
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Добавить товар в корзину
     */
    public void addToCart(String sessionId, Long productId, int quantity) {
        if (quantity <= 0) {
            log.warn("Попытка добавить товар с недопустимым количеством: {}", quantity);
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        log.info("Добавление товара ID={} в количестве {} в корзину сессии {}", productId, quantity, sessionId);

        Cart cart = getOrCreateCart(sessionId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Товар не найден с ID: {}", productId);
                    return new RuntimeException("Товар не найден с ID: " + productId);
                });

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.info("Обновлено количество товара ID={} в корзине. Новое количество: {}", productId, newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(newItem);
            cartRepository.save(cart);
            log.info("Добавлен новый товар ID={} в корзину сессии {}", productId, sessionId);
        }
    }

    /**
     * Обновить количество товара в корзине
     */
    public void updateQuantity(String sessionId, Long itemId, int quantity) {
        log.debug("Обновление количества для элемента корзины ID={}, количество={}", itemId, quantity);
        
        Cart cart = getOrCreateCart(sessionId);
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Элемент корзины ID={} не найден для обновления", itemId);
                    return new RuntimeException("Товар в корзине не найден");
                });

        if (quantity <= 0) {
            log.info("Количество товара ID={} <= 0, удаляем из корзины", itemId);
            removeFromCart(sessionId, itemId);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            log.info("Количество товара ID={} обновлено на {}", itemId, quantity);
        }
    }

    /**
     * Удалить конкретный товар из корзины
     */
    public void removeFromCart(String sessionId, Long itemId) {
        log.info("Удаление товара ID={} из корзины сессии {}", itemId, sessionId);
        
        Cart cart = getOrCreateCart(sessionId);
        
        CartItem itemToRemove = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Попытка удалить несуществующий товар ID={} из корзины", itemId);
                    return new RuntimeException("Товар в корзине не найден");
                });

        cart.getItems().remove(itemToRemove);
        cartItemRepository.delete(itemToRemove);
        cartRepository.save(cart);
        log.info("Товар ID={} успешно удален из корзины", itemId);
    }

    /**
     * Очистить корзину полностью
     */
    public void clearCart(String sessionId) {
        log.info("Полная очистка корзины для сессии {}", sessionId);
        
        Cart cart = getOrCreateCart(sessionId);
        int itemCount = cart.getItems().size();
        
        List<CartItem> itemsToDelete = List.copyOf(cart.getItems()); 
        cart.getItems().clear();
        cartItemRepository.deleteAll(itemsToDelete);
        cartRepository.save(cart);
        
        log.info("Корзина очищена. Удалено элементов: {}", itemCount);
    }

    /**
     * Привязать корзину к пользователю (при логине/регистрации)
     */
    public void mergeCartWithUser(String sessionId, Long userId) {
        log.debug("Попытка привязки корзины сессии {} к пользователю ID={}", sessionId, userId);
        
        Cart cart = cartRepository.findBySessionId(sessionId).orElse(null);
        if (cart != null && cart.getUser() == null) {
             log.info("Корзина сессии {} найдена, готова к привязке пользователя {}", sessionId, userId);
             // Логика слияния может быть добавлена здесь
        } else {
             log.debug("Корзина не найдена или уже привязана к пользователю");
        }
    }

    // --- Чтение и DTO ---

    /**
     * Получить корзину для отображения (DTO)
     */
    @Transactional(readOnly = true)
    public CartDto getCartDtoBySession(String sessionId) {
        log.debug("Запрос DTO корзины для сессии {}", sessionId);
        Cart cart = getOrCreateCart(sessionId);
        return mapToDto(cart);
    }

    /**
     * Получить сущность корзины (для внутренних операций)
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

        log.debug("Маппинг корзины завершен. Всего товаров: {}, Сумма: {}", dto.getTotalItems(), dto.getTotalAmount());
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