package com.example.bakery.feature.user.controller;

import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.order.service.OrderService;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    private final UserService userService;
    private final OrderService orderService;

    @GetMapping
    public String profilePage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String username = authentication.getName();
        log.debug("Запрос страницы профиля для пользователя: {}", username);

        try {
            // 1. Получаем данные пользователя
            UserDto userDto = userService.getUserByUsername(username);
            model.addAttribute("user", userDto);

            // 2. Получаем историю заказов пользователя
            // Используем сервис заказов, фильтруя по имени пользователя
            List<Order> userOrders = orderService.getAllOrders().stream()
                    .filter(order -> order.getUser() != null && order.getUser().getUsername().equals(username))
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())) // Сортировка: новые сверху
                    .collect(Collectors.toList());
            
            model.addAttribute("orders", userOrders);
            log.debug("Найдено заказов для профиля {}: {}", username, userOrders.size());

        } catch (Exception e) {
            log.error("Ошибка при загрузке профиля пользователя {}", username, e);
            model.addAttribute("error", "Не удалось загрузить данные профиля");
        }

        return "user/profile"; // Возвращает template: user/profile.html
    }
}