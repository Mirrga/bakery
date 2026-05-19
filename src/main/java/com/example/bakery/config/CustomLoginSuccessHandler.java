package com.example.bakery.config;

import com.example.bakery.feature.cart.service.CartService;
import com.example.bakery.feature.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomLoginSuccessHandler.class);

    private final CartService cartService;
    private final UserService userService;

    public CustomLoginSuccessHandler(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        String username = authentication.getName();

        try {
            // Получаем ID пользователя
            Long userId = userService.getUserByUsername(username).getId();
            
            log.info("Пользователь {} вошел. Сессия: {}. Запуск объединения корзин...", username, sessionId);
            
            // === ГЛАВНОЕ ДЕЙСТВИЕ ===
            cartService.mergeCartOnLogin(sessionId, userId);
            
        } catch (Exception e) {
            log.error("Ошибка при объединении корзин для пользователя {}", username, e);
        }

        // Продолжаем стандартный редирект
        super.onAuthenticationSuccess(request, response, authentication);
    }
}