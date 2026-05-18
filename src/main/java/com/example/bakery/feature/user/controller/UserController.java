package com.example.bakery.feature.user.controller;

import com.example.bakery.feature.user.dto.RegistrationRequest;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;
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

    // MVC View для регистрации
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userForm", new RegistrationRequest());
        return "user/register"; // Thymeleaf шаблон
    }

    // Обработка формы регистрации (MVC)
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userForm") RegistrationRequest request,
                                      BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "user/register";
        }
        try {
            userService.registerUser(request);
            return "redirect:/login?registered";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "user/register";
        }
    }

    // REST API endpoint (для AJAX или внешних клиентов)
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<UserDto> getUserApi(@PathVariable Long id) {
        try {
            UserDto user = userService.getUserByUsername("dummy"); // Заглушка, нужен поиск по ID в сервисе
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}