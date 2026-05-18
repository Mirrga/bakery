package com.example.bakery.feature.auth.service;

import com.example.bakery.feature.auth.entity.FormToken;
import com.example.bakery.feature.auth.repository.FormTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FormTokenService {

    private final FormTokenRepository formTokenRepository;

    /**
     * Генерирует новый токен для защиты формы
     */
    public String generateToken(String sessionId) {
        FormToken token = new FormToken();
        token.setToken(UUID.randomUUID().toString());
        token.setSessionId(sessionId);
        // Токен действителен 15 минут
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15)); 
        token.setUsed(false);
        
        formTokenRepository.save(token);
        return token.getToken();
    }

    /**
     * Проверяет и инвалидирует токен
     * @return true если токен валиден, false если нет
     */
    public boolean validateAndInvalidateToken(String token, String sessionId) {
        FormToken formToken = formTokenRepository.findByToken(token)
                .orElse(null);

        if (formToken == null) {
            return false;
        }

        // Проверка: использован ли уже
        if (formToken.isUsed()) {
            return false;
        }

        // Проверка: истек ли срок
        if (formToken.isExpired()) {
            // Опционально: можно удалить просроченный токен сразу
            formTokenRepository.delete(formToken);
            return false;
        }

        // Проверка: принадлежит ли токен текущей сессии (дополнительная защита)
        if (!formToken.getSessionId().equals(sessionId)) {
            return false;
        }

        // Помечаем как использованный
        formToken.setUsed(true);
        formTokenRepository.save(formToken);
        
        return true;
    }

    /**
     * Очистка старых токенов (можно вызывать из контроллера или по расписанию)
     */
    public void cleanupExpiredTokens() {
        formTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}