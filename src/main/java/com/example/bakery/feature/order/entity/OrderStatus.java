package com.example.bakery.feature.order.entity;

public enum OrderStatus {
    NEW,        // Новый
    CONFIRMED,  // Подтвержден
    READY,      // Готов к выдаче
    COMPLETED,  // Выдан
    CANCELLED   // Отменен
}