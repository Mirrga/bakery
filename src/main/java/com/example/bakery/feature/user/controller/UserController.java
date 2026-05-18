package com.example.bakery.feature.user.controller;

import com.example.bakery.feature.auth.dto.RegistrationRequest;
import com.example.bakery.feature.auth.service.FormTokenService;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FormTokenService formTokenService;

    // --- MVC Views ---

    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpSession session) {
        String token = formTokenService.generateToken(session.getId()); // Генерация токена
        model.addAttribute("formToken", token);
        model.addAttribute("userForm", new RegistrationRequest());
        return "user/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userForm") RegistrationRequest request,
                                    BindingResult bindingResult, Model model,
                                    HttpSession session,
                                    @RequestParam("formToken") String token) { // Добавлен параметр
        if (bindingResult.hasErrors()) {
            return "user/register";
        }
        try {
            String sessionId = session.getId();
            userService.registerUser(request, token, sessionId); // Передаем токен и ID сессии
            return "redirect:/login?registered";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userForm", request);
            return "user/register";
        }
    }

    // --- REST API Endpoints ---

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<UserDto> getUserApi(@PathVariable Long id) {
        try {
            // Исправлено: вызываем метод поиска по ID
            UserDto user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            // Возвращаем 404 если пользователь не найден или произошла ошибка
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Дополнительно: API для получения текущего пользователя (если нужно)
    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<UserDto> getCurrentUser() {
        // Логика получения текущего пользователя из SecurityContext
        // Пока заглушка, так как требует доработки сервиса
        return ResponseEntity.ok(null); 
    }
}