package com.example.bakery.feature.user.controller;

import com.example.bakery.feature.auth.dto.RegistrationRequest;
import com.example.bakery.feature.auth.service.FormTokenService;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final FormTokenService formTokenService;

    // --- MVC Views ---

    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpSession session) {
        log.debug("Запрос страницы регистрации");
        String token = formTokenService.generateToken(session.getId());
        model.addAttribute("formToken", token);
        model.addAttribute("userForm", new RegistrationRequest());
        return "user/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userForm") RegistrationRequest request,
                                    BindingResult bindingResult, Model model,
                                    HttpSession session,
                                    @RequestParam("formToken") String token) {
        
        String sessionId = session.getId();
        log.info("Попытка регистрации пользователя: {}", request.getEmail());

        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при регистрации: {}", bindingResult.getAllErrors());
            return "user/register";
        }

        try {
            userService.registerUser(request, token, sessionId);
            log.info("Пользователь {} успешно зарегистрирован", request.getEmail());
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка регистрации (данные): {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("userForm", request);
            return "user/register";
        } catch (RuntimeException e) {
            log.error("Ошибка регистрации (системная): {}", e.getMessage(), e);
            model.addAttribute("error", "Произошла ошибка при регистрации");
            model.addAttribute("userForm", request);
            return "user/register";
        }
    }

    // --- REST API Endpoints ---

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<UserDto> getUserApi(@PathVariable Long id) {
        log.debug("Запрос данных пользователя по ID: {}", id);
        try {
            UserDto user = userService.getUserById(id);
            log.debug("Пользователь найден: {}", user.getEmail());
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            log.warn("Пользователь с ID {} не найден: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Внутренняя ошибка при получении пользователя {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<UserDto> getCurrentUser() {
        log.debug("Запрос данных текущего пользователя");
        // Логика получения текущего пользователя из SecurityContext
        // Пока заглушка, так как требует доработки сервиса
        return ResponseEntity.ok(null); 
    }
}