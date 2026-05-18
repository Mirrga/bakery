package com.example.bakery.feature.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "form_tokens")
public class FormToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // Новое поле: ID сессии для привязки токена к пользователю
    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Новое поле: Явная дата истечения (опционально, но полезно для очистки)
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used;

    public FormToken() {
        this.createdAt = LocalDateTime.now();
        // Значение по умолчанию, будет перезаписано в сервисе
        this.expiryDate = createdAt.plusMinutes(15); 
        this.used = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    
    // Проверка истечения срока действия
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}