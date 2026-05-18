package com.example.bakery.feature.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.bakery.feature.user.entity.FormToken;

import java.util.Optional;
import java.util.List;

@Repository
public interface FormTokenRepository extends JpaRepository<FormToken, Long> {
    
    // Поиск токена по значению (старый метод, можно оставить для совместимости или удалить)
    Optional<FormToken> findByToken(String token);

    // Поиск активного токена для конкретной сессии (Безопасность!)
    Optional<FormToken> findByTokenAndSessionId(String token, String sessionId);

    // Удаление всех токенов сессии при выходе или очистке
    void deleteBySessionId(String sessionId);

    // Удаление старых неиспользованных токенов сессии перед генерацией нового (защита от мусора)
    void deleteBySessionIdAndUsedFalse(String sessionId);
    
    // Удаление по токену (если нужно вручную инвалидировать)
    void deleteByToken(String token);
}