package com.example.bakery.feature.cart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller; // <-- ВАЖНО: Используем @Controller, а не @RestController для всего класса
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

// Если оставить @RestController, ВСЕ методы будут возвращать JSON/текст.
// Для混合 (смешанного) контроллера лучше использовать @Controller 
// и добавлять @ResponseBody только там, где нужен JSON.
@Controller 
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // AJAX Endpoint: Добавить товар (возвращает JSON)
    @PostMapping("/add")
    @ResponseBody // <-- Явно указываем, что этот метод возвращает данные, а не страницу
    public ResponseEntity<Map<String, Object>> addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            HttpSession session) {
        
        String sessionId = session.getId();
        cartService.addToCart(sessionId, productId, quantity);
        
        Cart cart = cartService.getCartBySession(sessionId);
        int totalItems = cart.getItems().size();
        double totalPrice = cart.getTotalPrice().doubleValue();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "totalItems", totalItems,
            "totalPrice", totalPrice
        ));
    }

    // MVC Endpoint: Страница корзины (возвращает HTML)
    @GetMapping
    public String showCart(HttpSession session, Model model) {
        String sessionId = session.getId();
        Cart cart = cartService.getCartBySession(sessionId);
        model.addAttribute("cart", cart);
        
        // Эта строка должна совпадать с путем к файлу:
        // src/main/resources/templates/cart/view.html
        return "cart/view"; 
    }
}