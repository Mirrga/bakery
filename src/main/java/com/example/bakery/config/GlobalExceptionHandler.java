package com.example.bakery.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Единый обработчик для всех необработанных исключений
    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(HttpServletRequest request, Exception ex) {
        // Проверяем, ожидает ли клиент JSON (AJAX запросы обычно шлют Accept: application/json)
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = (acceptHeader != null && acceptHeader.contains("application/json"));

        if (isJsonRequest) {
            return handleJsonError(ex);
        } else {
            return handleHtmlError(request, ex);
        }
    }

    private ResponseEntity<Map<String, String>> handleJsonError(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Внутренняя ошибка сервера");
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Неизвестная ошибка");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ModelAndView handleHtmlError(HttpServletRequest request, Exception ex) {
        ModelAndView mav = new ModelAndView("error/custom-error");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject("error", "Внутренняя ошибка");
        mav.addObject("message", ex.getMessage() != null ? ex.getMessage() : "Произошла непредвиденная ошибка");
        mav.addObject("path", request.getRequestURI());
        return mav;
    }
    
    // Обработчик специально для ошибок валидации (если нужно отдельное поведение)
    // org.springframework.web.bind.MethodArgumentNotValidException
    // Можно добавить отдельно, если требуется специфичная обработка
}