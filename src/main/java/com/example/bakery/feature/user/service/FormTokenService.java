package com.example.bakery.feature.user.service;

import com.example.bakery.feature.user.entity.FormToken;
import com.example.bakery.feature.user.repository.FormTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class FormTokenService {

    private final FormTokenRepository tokenRepository;

    // Внедряем репозиторий через конструктор (лучшая практика, чем @Autowired)
    public FormTokenService(FormTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Генерирует новый токен и привязывает его к текущей сессии пользователя.
     */
    public String generateToken(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        
        // Очистим старые неиспользованные токены для этой сессии (опционально, для чистоты)
        tokenRepository.deleteBySessionIdAndUsedFalse(sessionId);

        FormToken formToken = new FormToken();
        formToken.setToken(UUID.randomUUID().toString());
        formToken.setSessionId(sessionId);
        formToken.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // Токен живет 15 минут
        formToken.setUsed(false);

        tokenRepository.save(formToken);
        return formToken.getToken();
    }

    /**
     * Проверяет токен и помечает его как использованный (или удаляет).
     * Возвращает true только если токен валиден, принадлежит сессии и еще не был использован.
     */
    public boolean validateAndRemoveToken(HttpServletRequest request, String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String sessionId = request.getSession().getId();
        
        // Ищем токен по значению и сессии
        FormToken formToken = tokenRepository.findByTokenAndSessionId(token, sessionId)
                .orElse(null);

        if (formToken == null) {
            return false; // Токен не найден или не принадлежит этой сессии
        }

        // Проверка на истечение срока
        if (formToken.isExpired()) {
            tokenRepository.delete(formToken); // Удаляем просроченный
            return false;
        }

        // Проверка на повторное использование
        if (formToken.isUsed()) {
            return false; // Уже был использован
        }

        // Помечаем как использованный (или можно сразу удалить для одноразовых токенов)
        formToken.setUsed(true);
        tokenRepository.save(formToken);
        
        return true;
    }
    
    /**
     * Принудительная инвалидация всех токенов сессии (например, при логауте).
     */
    public void invalidateSessionTokens(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        tokenRepository.deleteBySessionId(sessionId);
    }
}