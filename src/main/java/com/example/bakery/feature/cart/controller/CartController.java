package com.example.bakery.feature.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bakery.feature.cart.dto.CartDto;
import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // --- MVC Endpoints (возвращают HTML страницы) ---

    /**
     * Страница просмотра корзины
     */
    @GetMapping
    public String showCart(HttpSession session, Model model) {
        String sessionId = session.getId();
        // Используем DTO для безопасной передачи данных в шаблон
        CartDto cartDto = cartService.getCartDtoBySession(sessionId);
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
        
        try {
            String sessionId = session.getId();
            cartService.addToCart(sessionId, productId, quantity);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Товар добавлен в корзину",
                "totalItems", cartDto.getTotalItems(),
                "totalAmount", cartDto.getTotalAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
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
        
        try {
            String sessionId = session.getId();
            // Если quantity <= 0, сервис автоматически удалит товар
            cartService.updateQuantity(sessionId, itemId, quantity);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Количество обновлено",
                "totalItems", cartDto.getTotalItems(),
                "totalAmount", cartDto.getTotalAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
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
        
        try {
            String sessionId = session.getId();
            cartService.removeFromCart(sessionId, itemId);
            
            CartDto cartDto = cartService.getCartDtoBySession(sessionId);
            
            // Если корзина пуста, можно вернуть редирект или просто обновить интерфейс
            boolean isEmpty = cartDto.getItems().isEmpty();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Товар удален",
                "totalItems", cartDto.getTotalItems(),
                "totalAmount", cartDto.getTotalAmount(),
                "isEmpty", isEmpty
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Очистить всю корзину (AJAX)
     */
    @PostMapping("/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCart(HttpSession session) {
        try {
            String sessionId = session.getId();
            cartService.clearCart(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Корзина очищена",
                "totalItems", 0,
                "totalAmount", 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}