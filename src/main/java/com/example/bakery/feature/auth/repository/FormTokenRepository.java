package com.example.bakery.feature.auth.repository;

import com.example.bakery.feature.auth.entity.FormToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FormTokenRepository extends JpaRepository<FormToken, Long> {
    
    Optional<FormToken> findByToken(String token);
    
    // Поиск по сессии (если нужно проверить активные токены сессии)
    List<FormToken> findBySessionIdAndUsedFalse(String sessionId);
    
    // Метод для очистки старых токенов (можно запускать по расписанию)
    void deleteByExpiryDateBefore(LocalDateTime date);
}