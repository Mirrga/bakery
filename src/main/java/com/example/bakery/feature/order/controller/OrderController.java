package com.example.bakery.feature.order.controller;

import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.service.CartService;
import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.order.entity.OrderStatus;
import com.example.bakery.feature.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final CartService cartService;

    /**
     * Страница оформления заказа (форма с данными доставки)
     */
    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        String sessionId = session.getId();
        Cart cart = cartService.getCartBySession(sessionId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart?empty=true";
        }

        // Принудительная инициализация ленивой загрузки
        try {
            cart.getItems().size(); 
            for (var item : cart.getItems()) {
                item.getProduct().getName(); 
            }
        } catch (Exception e) {
            log.error("Ошибка инициализации данных корзины", e);
            return "redirect:/cart?error=init";
        }

        // === РАССЧИТЫВАЕМ СУММУ В КОНТРОЛЛЕРЕ ===
        BigDecimal totalAmount = cart.getItems().stream()
            .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("cart", cart);
        model.addAttribute("totalAmount", totalAmount); // Передаем готовую сумму
        // =========================================

        return "order/checkout";
    }

    @PostMapping("/create")
    public String createOrder(HttpSession session, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        String sessionId = session.getId();

        try {
            Order order = orderService.createOrderFromCart(sessionId, username);
            redirectAttributes.addFlashAttribute("success", "Заказ №" + order.getId() + " успешно оформлен!");
            return "redirect:/orders/" + order.getId();
        } catch (RuntimeException e) {
            log.error("Ошибка создания заказа", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Страница конкретного заказа
     * Доступно авторизованным пользователям (просмотр своих заказов реализован внутри сервиса или через проверку в шаблоне)
     */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        try {
            log.debug("Просмотр заказа №{}", id);
            Order order = orderService.getOrderById(id);
            
            // Простая проверка: если пользователь не админ и заказ не его, запрещаем просмотр
            if (!order.getUser().getUsername().equals(authentication.getName()) 
                && !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                log.warn("Пользователь {} попытался получить доступ к чужому заказу №{}", authentication.getName(), id);
                return "redirect:/orders/my?accessDenied";
            }

            double usdRate = orderService.getCurrentUsdRate();
            BigDecimal totalInUsd = BigDecimal.ZERO;
            
            if (usdRate > 0) {
                totalInUsd = order.getTotalAmount().multiply(BigDecimal.valueOf(usdRate));
                model.addAttribute("showUsd", true);
                model.addAttribute("currentUsdRate", usdRate);
                model.addAttribute("totalInUsd", totalInUsd);
                log.debug("Для заказа {} добавлен курс USD: {}", id, usdRate);
            } else {
                model.addAttribute("showUsd", false);
            }
            
            model.addAttribute("order", order);
            return "order/view";
        } catch (RuntimeException e) {
            log.warn("Заказ №{} не найден: {}", id, e.getMessage());
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
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        log.debug("Запрос списка заказов пользователя: {}", username);
        
        List<Order> orders = orderService.getAllOrders().stream()
                .filter(o -> o.getUser().getUsername().equals(username))
                .toList();
        
        log.debug("Найдено заказов для пользователя {}: {}", username, orders.size());
        model.addAttribute("orders", orders);
        return "order/list";
    }

    /**
     * Админка: Список всех заказов
     * Доступ ТОЛЬКО для роли ADMIN
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrders(Model model) {
        log.debug("Администратор запросил список всех заказов");
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        return "order/admin";
    }

    /**
     * Админка: Обновление статуса заказа
     * Доступ ТОЛЬКО для роли ADMIN
     */
    @PostMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            log.info("Администратор изменяет статус заказа №{} на {}", id, status);
            orderService.updateOrderStatus(id, status);
            log.info("Статус заказа №{} успешно обновлен на {}", id, status);
            redirectAttributes.addFlashAttribute("success", "Статус заказа обновлен");
        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении статуса заказа №{} на {}: {}", id, status, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении статуса: " + e.getMessage());
        }
        return "redirect:/orders/admin";
    }
}