package com.example.bakery.feature.cart.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.bakery.feature.cart.dto.CartDto;
import com.example.bakery.feature.cart.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    // --- MVC Endpoints (возвращают HTML страницы) ---

    /**
     * Страница просмотра корзины
     */
    @GetMapping
    public String showCart(HttpSession session, Model model) {
        String sessionId = session.getId();
        log.debug("Запрос просмотра корзины для сессии: {}", sessionId);
        
        // Используем DTO для безопасной передачи данных в шаблон
        CartDto cartDto = cartService.getCartDtoBySession(sessionId);
        
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
        log.info("Попытка добавить товар в корзину. Session: {}, ProductId: {}, Quantity: {}", 
                sessionId, productId, quantity);
        
        try {
            cartService.addToCart(sessionId, productId, quantity);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId);
            
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
        log.info("Обновление количества товара. Session: {}, ItemId: {}, NewQuantity: {}", 
                sessionId, itemId, quantity);
        
        try {
            // Если quantity <= 0, сервис автоматически удалит товар
            cartService.updateQuantity(sessionId, itemId, quantity);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId);
            
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
        log.info("Удаление товара из корзины. Session: {}, ItemId: {}", sessionId, itemId);
        
        try {
            cartService.removeFromCart(sessionId, itemId);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId);
            
            // Если корзина пуста, можно вернуть редирект или просто обновить интерфейс
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
        log.info("Очистка всей корзины. Session: {}", sessionId);
        
        try {
            cartService.clearCart(sessionId);
            
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