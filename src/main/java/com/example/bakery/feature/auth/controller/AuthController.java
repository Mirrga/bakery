package com.example.bakery.feature.auth.controller;

import com.example.bakery.feature.auth.dto.RegistrationRequest;
import com.example.bakery.feature.auth.service.FormTokenService;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final FormTokenService formTokenService;

    // Внедрение зависимостей через конструктор
    public AuthController(UserService userService, FormTokenService formTokenService) {
        this.userService = userService;
        this.formTokenService = formTokenService;
    }

    // ================= MVC CONTROLLER METHODS =================

    /**
     * Страница входа
     */
    @GetMapping("/login")
    public String showLoginPage(Model model, HttpServletRequest request) {
        log.debug("Запрос страницы входа");
        String token = formTokenService.generateToken(request.getSession().getId());
        model.addAttribute("formToken", token);
        return "auth/login";
    }

    /**
     * Страница регистрации
     */
    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        log.debug("Запрос страницы регистрации");
        String token = formTokenService.generateToken(request.getSession().getId());
        model.addAttribute("formToken", token);
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register";
    }

    /**
     * Обработка формы регистрации (MVC)
     */
    @PostMapping("/register")
    public String processRegistration(@RequestParam("formToken") String formToken,
                                      @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
                                      BindingResult bindingResult,
                                      Model model,
                                      HttpServletRequest httpRequest) {
        
        String sessionId = httpRequest.getSession().getId();
        log.info("Попытка регистрации пользователя: {}", request.getEmail());

        // 1. Проверка токена защиты от повторной отправки
        if (!formTokenService.validateAndInvalidateToken(formToken, sessionId)) {
            log.warn("Отклонена повторная отправка формы регистрации для сессии {}", sessionId);
            model.addAttribute("error", "Повторная отправка формы или сессия истекла. Пожалуйста, обновите страницу.");
            model.addAttribute("formToken", formTokenService.generateToken(sessionId));
            return "auth/register";
        }

        // 2. Проверка валидации данных (@Valid)
        if (bindingResult.hasErrors()) {
            log.warn("Ошибка валидации данных регистрации: {}", bindingResult.getAllErrors());
            model.addAttribute("formToken", formTokenService.generateToken(sessionId));
            return "auth/register";
        }

        // 3. Бизнес-логика регистрации
        try {
            UserDto userDto = userService.registerUser(request, formToken, sessionId);
            log.info("Пользователь успешно зарегистрирован: {} (ID={})", userDto.getEmail(), userDto.getId());
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка регистрации (бизнес-правила): {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formToken", formTokenService.generateToken(sessionId));
            return "auth/register";
        } catch (Exception e) {
            log.error("Неожиданная ошибка при регистрации пользователя {}", request.getEmail(), e);
            model.addAttribute("error", "Произошла системная ошибка. Попробуйте позже.");
            model.addAttribute("formToken", formTokenService.generateToken(sessionId));
            return "auth/register";
        }
    }

    // ================= REST API CONTROLLER METHODS =================

    /**
     * REST API для регистрации (AJAX)
     * Требует передачи токена в заголовке X-Form-Token или параметре formToken
     */
    @PostMapping("/api/register")
    public ResponseEntity<?> registerApi(@RequestBody RegistrationRequest request,
                                         @RequestHeader(value = "X-Form-Token", required = false) String headerToken,
                                         @RequestParam(value = "formToken", required = false) String paramToken,
                                         HttpServletRequest httpRequest) {
        
        String sessionId = httpRequest.getSession().getId();
        // Токен может прийти либо в заголовке, либо в параметре
        String token = (headerToken != null) ? headerToken : paramToken;

        log.debug("API запрос регистрации от IP: {}, Session: {}", httpRequest.getRemoteAddr(), sessionId);

        if (token == null || token.isEmpty()) {
            log.warn("API регистрация отклонена: отсутствует токен безопасности");
            return ResponseEntity.badRequest().body("Отсутствует токен безопасности формы");
        }

        try {
            // Вызываем сервис с токеном и sessionId
            UserDto userDto = userService.registerUser(request, token, sessionId);
            
            log.info("API регистрация успешна: {} (ID={})", userDto.getEmail(), userDto.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Регистрация успешна");
            response.put("user", userDto);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("API регистрация отклонена (бизнес-правила): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("API регистрация: критическая ошибка сервера", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Внутренняя ошибка сервера"
            ));
        }
    }
}