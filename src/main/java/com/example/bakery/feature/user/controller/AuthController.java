package com.example.bakery.feature.user.controller;

import com.example.bakery.feature.user.dto.RegistrationRequest;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;
import com.example.bakery.feature.user.service.FormTokenService; // Импортируем сервис токенов

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final FormTokenService formTokenService; // Добавляем поле

    // Внедряем оба сервиса через конструктор
    public AuthController(UserService userService, FormTokenService formTokenService) {
        this.userService = userService;
        this.formTokenService = formTokenService;
    }

    // Страница входа (Thymeleaf)
    @GetMapping("/login")
    public String showLoginPage(Model model, HttpServletRequest request) {
        // Генерируем токен для защиты формы входа
        String token = formTokenService.generateToken(request);
        model.addAttribute("formToken", token);
        
        return "auth/login"; 
    }

    // Страница регистрации (Thymeleaf)
    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        // Просто добавляем атрибуты и возвращаем имя файла templates/auth/register.html
        String token = formTokenService.generateToken(request);
        model.addAttribute("formToken", token);
        model.addAttribute("registrationRequest", new RegistrationRequest());
        
        // ВАЖНО: Никаких redirect здесь!
        return "auth/register"; 
    }

    // Обработка формы регистрации (MVC)
    @PostMapping("/register")
    public String processRegistration(@RequestParam("formToken") String formToken,
                                      @Valid @ModelAttribute RegistrationRequest request,
                                      BindingResult bindingResult,
                                      Model model,
                                      HttpServletRequest httpServletRequest) {
        
        // 1. Проверка токена защиты от повторной отправки
        if (!formTokenService.validateAndRemoveToken(httpServletRequest, formToken)) {
            model.addAttribute("error", "Повторная отправка формы или сессия истекла.");
            // Генерируем новый токен для отображения формы снова
            model.addAttribute("formToken", formTokenService.generateToken(httpServletRequest));
            return "auth/register";
        }

        // 2. Проверка валидации данных (@Valid)
        if (bindingResult.hasErrors()) {
            model.addAttribute("formToken", formTokenService.generateToken(httpServletRequest));
            return "auth/register";
        }

        // 3. Бизнес-логика
        try {
            userService.registerUser(request);
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formToken", formTokenService.generateToken(httpServletRequest));
            return "auth/register";
        }
    }

    // REST API для регистрации (для AJAX/jQuery)
    @PostMapping("/api/register")
    public ResponseEntity<?> registerApi(@Valid @RequestBody RegistrationRequest request) {
        try {
            UserDto userDto = userService.registerUser(request);
            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/users/{id}")
                    .buildAndExpand(userDto.getId())
                    .toUri();
            return ResponseEntity.created(location).body(userDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}