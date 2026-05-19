package com.example.bakery.feature.cart.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.bakery.feature.cart.dto.CartDto;
import com.example.bakery.feature.cart.service.CartService;
import com.example.bakery.feature.user.service.UserService;
import com.example.bakery.feature.user.dto.UserDto;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService; // Внедряем сервис пользователей

    /**
     * Получает ID текущего авторизованного пользователя.
     * Если пользователь не авторизован (гость), возвращает null.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        
        String username = authentication.getName();
        try {
            UserDto user = userService.getUserByUsername(username);
            return user.getId();
        } catch (Exception e) {
            log.warn("Не удалось получить ID пользователя по имени {}: {}", username, e.getMessage());
            return null;
        }
    }

    // --- MVC Endpoints (возвращают HTML страницы) ---

    /**
     * Страница просмотра корзины
     */
    @GetMapping
    public String showCart(HttpSession session, Model model) {
        String sessionId = session.getId();
        Long userId = getCurrentUserId();
        
        log.debug("Запрос просмотра корзины. Session: {}, User: {}", sessionId, userId != null ? userId : "Guest");
        
        // Передаем userId в сервис для получения правильной корзины
        CartDto cartDto = cartService.getCartDtoBySession(sessionId, userId);
        
        log.info("Корзина отображена. Товаров: {}, Сумма: {}", 
                cartDto.getTotalItems(), cartDto.getTotalAmount());
        
        model.addAttribute("cart", cartDto);
        return "cart/view";
    }

    // --- REST/AJAX Endpoints (возвращают JSON) ---

    /**
     * Добавить товар в корзину (AJAX)
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {
        
        String sessionId = session.getId();
        Long userId = getCurrentUserId();
        
        log.info("Попытка добавить товар в корзину. Session: {}, User: {}, ProductId: {}, Quantity: {}", 
                sessionId, userId != null ? userId : "Guest", productId, quantity);
        
        try {
            // Передаем userId в сервис
            cartService.addToCart(sessionId, productId, quantity, userId);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId, userId);
            
            log.debug("Товар успешно добавлен. Всего товаров: {}, Сумма: {}", 
                    cartDto.getTotalItems(), cartDto.getTotalAmount());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Товар добавлен в корзину",
                "totalItems", cartDto.getTotalItems(),
                "totalAmount", cartDto.getTotalAmount()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при добавлении в корзину: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Неожиданная ошибка при добавлении товара в корзину", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Произошла ошибка при обработке запроса"
            ));
        }
    }

    /**
     * Обновить количество товара (AJAX)
     */
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateQuantity(
            @RequestParam Long itemId,
            @RequestParam int quantity,
            HttpSession session) {
        
        String sessionId = session.getId();
        Long userId = getCurrentUserId();
        
        log.info("Обновление количества товара. Session: {}, User: {}, ItemId: {}, NewQuantity: {}", 
                sessionId, userId != null ? userId : "Guest", itemId, quantity);
        
        try {
            // Передаем userId в сервис
            cartService.updateQuantity(sessionId, itemId, quantity, userId);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId, userId);
            
            log.debug("Количество обновлено. Всего товаров: {}, Сумма: {}", 
                    cartDto.getTotalItems(), cartDto.getTotalAmount());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Количество обновлено",
                "totalItems", cartDto.getTotalItems(),
                "totalAmount", cartDto.getTotalAmount()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка при обновлении количества: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Ошибка при обновлении количества товара", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Произошла ошибка при обновлении"
            ));
        }
    }

    /**
     * Удалить товар из корзины (AJAX)
     */
    @PostMapping("/remove/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable Long itemId,
            HttpSession session) {
        
        String sessionId = session.getId();
        Long userId = getCurrentUserId();
        
        log.info("Удаление товара из корзины. Session: {}, User: {}, ItemId: {}", 
                sessionId, userId != null ? userId : "Guest", itemId);
        
        try {
            // Передаем userId в сервис
            cartService.removeFromCart(sessionId, itemId, userId);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId, userId);
            
            boolean isEmpty = cartDto.getItems().isEmpty();
            
            if (isEmpty) {
                log.info("Корзина стала пустой после удаления товара");
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Товар удален",
                "totalItems", cartDto.getTotalItems(),
                "totalAmount", cartDto.getTotalAmount(),
                "isEmpty", isEmpty
            ));
        } catch (Exception e) {
            log.error("Ошибка при удалении товара из корзины", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Произошла ошибка при удалении"
            ));
        }
    }

    /**
     * Очистить всю корзину (AJAX)
     */
    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCart(HttpSession session) {
        String sessionId = session.getId();
        Long userId = getCurrentUserId();
        
        log.info("Очистка всей корзины. Session: {}, User: {}", 
                sessionId, userId != null ? userId : "Guest");
        
        try {
            // Передаем userId в сервис
            cartService.clearCart(sessionId, userId);
            
            log.info("Корзина успешно очищена");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Корзина очищена",
                "totalItems", 0,
                "totalAmount", 0
            ));
        } catch (Exception e) {
            log.error("Ошибка при очистке корзины", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Произошла ошибка при очистке"
            ));
        }
    }
}