package com.example.bakery.feature.order.controller;

import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.service.CartService;
import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.order.entity.OrderStatus;
import com.example.bakery.feature.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    /**
     * Оформление заказа (POST запрос)
     */
    @PostMapping("/create")
    public String createOrder(HttpSession session,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Для оформления заказа необходимо войти в систему");
            return "redirect:/login";
        }

        String username = authentication.getName();
        String sessionId = session.getId();

        try {
            Order order = orderService.createOrderFromCart(sessionId, username);
            redirectAttributes.addFlashAttribute("success", "Заказ №" + order.getId() + " успешно оформлен!");
            return "redirect:/orders/" + order.getId();
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Страница конкретного заказа
     */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        try {
            Order order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            return "order/view";
        } catch (RuntimeException e) {
            model.addAttribute("error", "Заказ не найден");
            return "error";
        }
    }

    /**
     * Страница со всеми заказами пользователя
     */
    @GetMapping("/my")
    public String myOrders(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        List<Order> orders = orderService.getUserOrders(username);
        model.addAttribute("orders", orders);
        return "order/list";
    }

    /**
     * Страница оформления заказа (форма с данными доставки)
     */
    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        String sessionId = session.getId();
        Cart cart = cartService.getCartBySession(sessionId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            model.addAttribute("error", "Ваша корзина пуста");
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        return "order/checkout";
    }

    /**
     * Админка: Обновление статуса заказа
     */
    @PostMapping("/admin/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Статус заказа обновлен");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении статуса: " + e.getMessage());
        }
        return "redirect:/orders/admin";
    }

    /**
     * Админка: Список всех заказов
     */
    @GetMapping("/admin")
    public String adminOrders(Model model) {
        // В реальном проекте здесь нужна проверка роли ADMIN
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        return "order/admin";
    }
}