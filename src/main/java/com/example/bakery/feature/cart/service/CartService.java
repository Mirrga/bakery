package com.example.bakery.feature.cart.service;

import com.example.bakery.feature.cart.dto.CartDto;
import com.example.bakery.feature.cart.dto.CartItemDto;
import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.entity.CartItem;
import com.example.bakery.feature.cart.repository.CartItemRepository;
import com.example.bakery.feature.cart.repository.CartRepository;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.ProductRepository;
import com.example.bakery.feature.user.entity.User; // Убедитесь, что импорт есть
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
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // --- Основные операции с корзиной ---

    /**
     * Получить или создать корзину.
     * Логика изменена: если передан userId (пользователь авторизован), ищем корзину по нему.
     * Если нет - ищем по sessionId.
     */
    @Transactional
    public Cart getOrCreateCart(String sessionId, Long userId) {
        log.debug("Поиск корзины. SessionId: {}, UserId: {}", sessionId, userId);

        // 1. Если пользователь авторизован, приоритет у его личной корзины
        if (userId != null) {
            Optional<Cart> userCart = cartRepository.findByUserId(userId);
            if (userCart.isPresent()) {
                log.debug("Найдена корзина пользователя ID: {}", userCart.get().getId());
                // Опционально: можно обновить sessionId в корзине пользователя на текущий, 
                // чтобы не терять связь, если сессия изменилась
                userCart.get().setSessionId(sessionId);
                return cartRepository.save(userCart.get());
            }
        }

        // 2. Если личной корзины нет или пользователь гость, ищем по sessionId
        return cartRepository.findBySessionIdWithItems(sessionId)
                .orElseGet(() -> {
                    log.info("Корзина не найдена, создаем новую для сессии: {}", sessionId);
                    Cart newCart = new Cart();
                    newCart.setSessionId(sessionId);
                    if (userId != null) {
                        // Сразу привязываем к пользователю, если он известен
                        User user = new User();
                        user.setId(userId);
                        newCart.setUser(user);
                        log.info("Новая корзина сразу привязана к пользователю {}", userId);
                    }
                    return cartRepository.save(newCart);
                });
    }

    // Перегруженный метод для обратной совместимости (если где-то вызывается без userId)
    public Cart getOrCreateCart(String sessionId) {
        return getOrCreateCart(sessionId, null);
    }

    /**
     * Добавить товар в корзину
     */
    @Transactional
    public void addToCart(String sessionId, Long productId, int quantity, Long userId) {
        if (quantity <= 0) {
            log.warn("Попытка добавить товар с недопустимым количеством: {}", quantity);
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        log.info("Добавление товара ID={} в корзину. Session: {}, User: {}", productId, sessionId, userId);

        // Используем обновленный метод с userId
        Cart cart = getOrCreateCart(sessionId, userId);
        
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
            log.info("Добавлен новый товар ID={} в корзину", productId);
        }
    }

    // Перегрузка для обратной совместимости
    public void addToCart(String sessionId, Long productId, int quantity) {
        addToCart(sessionId, productId, quantity, null);
    }

    /**
     * Обновить количество товара в корзине
     */
    @Transactional
    public void updateQuantity(String sessionId, Long itemId, int quantity, Long userId) {
        log.debug("Обновление количества для элемента корзины ID={}, количество={}", itemId, quantity);
        
        Cart cart = getOrCreateCart(sessionId, userId);
        
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Элемент корзины ID={} не найден для обновления", itemId);
                    return new RuntimeException("Товар в корзине не найден");
                });

        if (quantity <= 0) {
            log.info("Количество товара ID={} <= 0, удаляем из корзины", itemId);
            removeFromCart(sessionId, itemId, userId);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            log.info("Количество товара ID={} обновлено на {}", itemId, quantity);
        }
    }

    public void updateQuantity(String sessionId, Long itemId, int quantity) {
        updateQuantity(sessionId, itemId, quantity, null);
    }

    /**
     * Удалить конкретный товар из корзины
     */
    @Transactional
    public void removeFromCart(String sessionId, Long itemId, Long userId) {
        log.info("Удаление товара ID={} из корзины", itemId);
        
        Cart cart = getOrCreateCart(sessionId, userId);
        
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

    public void removeFromCart(String sessionId, Long itemId) {
        removeFromCart(sessionId, itemId, null);
    }

    /**
     * Очистить корзину полностью
     */
    @Transactional
    public void clearCart(String sessionId, Long userId) {
        log.info("Полная очистка корзины");
        
        Cart cart = getOrCreateCart(sessionId, userId);
        int itemCount = cart.getItems().size();
        
        List<CartItem> itemsToDelete = List.copyOf(cart.getItems()); 
        cart.getItems().clear();
        cartItemRepository.deleteAll(itemsToDelete);
        cartRepository.save(cart);
        
        log.info("Корзина очищена. Удалено элементов: {}", itemCount);
    }

    public void clearCart(String sessionId) {
        clearCart(sessionId, null);
    }

    /**
     * === НОВАЯ ФИЧА: Объединение корзин при входе ===
     * Вызывать этот метод после успешной аутентификации пользователя.
     */
    @Transactional
    public void mergeCartOnLogin(String sessionId, Long userId) {
        log.info("Начало объединения корзин при входе. User: {}, Session: {}", userId, sessionId);

        // 1. Ищем корзину, которая уже принадлежит пользователю (сохраненная в БД ранее)
        Optional<Cart> userCartOpt = cartRepository.findByUserId(userId);

        // 2. Ищем текущую гостевую корзину по sessionId
        Optional<Cart> guestCartOpt = cartRepository.findBySessionIdWithItems(sessionId);

        if (!guestCartOpt.isPresent()) {
            // Если гостевой корзины нет, просто убеждаемся, что у пользователя есть корзина
            if (userCartOpt.isEmpty()) {
                log.info("Гостевая корзина не найдена. Создаем пустую корзину для пользователя {}", userId);
                Cart newCart = new Cart();
                newCart.setSessionId(sessionId);
                User user = new User();
                user.setId(userId);
                newCart.setUser(user);
                cartRepository.save(newCart);
            } else {
                // Привязываем существующую корзину пользователя к текущей сессии
                Cart userCart = userCartOpt.get();
                userCart.setSessionId(sessionId);
                cartRepository.save(userCart);
                log.info("Существующая корзина пользователя {} привязана к сессии {}", userId, sessionId);
            }
            return;
        }

        Cart guestCart = guestCartOpt.get();

        // Если гостевая корзина уже принадлежит этому пользователю, ничего делать не надо
        if (guestCart.getUser() != null && guestCart.getUser().getId().equals(userId)) {
            log.info("Гостевая корзина уже принадлежит пользователю. Объединение не требуется.");
            return;
        }

        if (userCartOpt.isPresent()) {
            // Сценарий А: У пользователя уже есть сохраненная корзина.
            // Нужно перенести товары из гостевой корзины в пользовательскую.
            Cart userCart = userCartOpt.get();
            log.info("Найдена существующая корзина пользователя. Объединяем с гостевой...");
            
            mergeCarts(userCart, guestCart);
            
            // Сохраняем обновленную пользовательскую корзину
            userCart.setSessionId(sessionId); // Обновляем сессию
            cartRepository.save(userCart);
            
            // Удаляем гостевую корзину (товары уже перенесены или удалены через cascade, если настроено, но лучше явно почистить ссылки)
            // Важно: мы уже перенесли элементы в userCart, поэтому guestCart.items можно очистить перед удалением, 
            // чтобы не сработало каскадное удаление товаров, если они共享ятся (хотя в нашей логике мы создали новые связи или обновили старые)
            // В нашем методе mergeCarts мы меняем ссылку cart у элементов на userCart.
            guestCart.getItems().clear(); 
            cartRepository.delete(guestCart);
            
            log.info("Корзины успешно объединены. ID результирующей корзины: {}", userCart.getId());
        } else {
            // Сценарий Б: У пользователя нет сохраненной корзины.
            // Просто привязываем гостевую корзину к пользователю.
            log.info("Сохраненная корзина пользователя не найдена. Привязываем гостевую корзину к пользователю {}", userId);
            
            User user = new User();
            user.setId(userId);
            guestCart.setUser(user);
            guestCart.setSessionId(sessionId);
            
            cartRepository.save(guestCart);
            log.info("Гостевая корзина ID={} привязана к пользователю", guestCart.getId());
        }
    }

    /**
     * Вспомогательный метод для переноса товаров из sourceCart в targetCart
     */
    private void mergeCarts(Cart targetCart, Cart sourceCart) {
        if (sourceCart.getItems() == null || sourceCart.getItems().isEmpty()) {
            return;
        }

        for (CartItem sourceItem : sourceCart.getItems()) {
            Product product = sourceItem.getProduct();
            int quantityToAdd = sourceItem.getQuantity();

            // Ищем такой же товар в целевой корзине
            Optional<CartItem> existingTargetItem = targetCart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(product.getId()))
                    .findFirst();

            if (existingTargetItem.isPresent()) {
                // Если товар уже есть, увеличиваем количество
                CartItem targetItem = existingTargetItem.get();
                targetItem.setQuantity(targetItem.getQuantity() + quantityToAdd);
                cartItemRepository.save(targetItem);
                log.debug("Обновлено количество товара {} в объединенной корзине", product.getName());
            } else {
                // Если товара нет, переносим элемент
                sourceItem.setCart(targetCart);
                targetCart.getItems().add(sourceItem);
                // Не сохраняем каждый элемент отдельно, saveAll или save(targetCart) сделает это позже
                log.debug("Товар {} добавлен в объединенную корзину", product.getName());
            }
        }
        // Сохраняем изменения в элементах целевой корзины
        if (!targetCart.getItems().isEmpty()) {
            cartItemRepository.saveAll(targetCart.getItems());
        }
    }

    // --- Чтение и DTO ---

    @Transactional
    public CartDto getCartDtoBySession(String sessionId, Long userId) {
        log.debug("Запрос DTO корзины. Session: {}, User: {}", sessionId, userId);
        Cart cart = getOrCreateCart(sessionId, userId);
        return mapToDto(cart);
    }

    public CartDto getCartDtoBySession(String sessionId) {
        return getCartDtoBySession(sessionId, null);
    }

    @Transactional
    public Cart getCartBySession(String sessionId, Long userId) {
        return getOrCreateCart(sessionId, userId);
    }

    public Cart getCartBySession(String sessionId) {
        return getCartBySession(sessionId, null);
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
            if (item.getProduct() != null) {
                itemDto.setProductId(item.getProduct().getId());
                itemDto.setProductName(item.getProduct().getName());
                itemDto.setProductImageUrl(item.getProduct().getImageUrl());
                itemDto.setPrice(item.getProduct().getPrice());
            } else {
                // Обработка случая, если товар был удален из БД, но остался в корзине
                itemDto.setProductName("Товар удален");
                itemDto.setPrice(BigDecimal.ZERO);
            }
            itemDto.setQuantity(item.getQuantity());
            
            BigDecimal subtotal = (itemDto.getPrice() != null) 
                    ? itemDto.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    : BigDecimal.ZERO;
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