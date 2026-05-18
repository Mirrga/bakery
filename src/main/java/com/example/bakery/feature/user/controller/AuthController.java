package com.example.bakery.feature.user.controller;

import com.example.bakery.feature.user.dto.RegistrationRequest;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Страница входа (Thymeleaf)
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login"; // Шаблон templates/auth/login.html
    }

    // Страница регистрации (Thymeleaf)
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register"; // Шаблон templates/auth/register.html
    }

    // Обработка формы регистрации (MVC)
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute RegistrationRequest request,
                                      BindingResult bindingResult,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(request);
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
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