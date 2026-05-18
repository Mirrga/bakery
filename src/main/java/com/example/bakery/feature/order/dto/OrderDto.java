package com.example.bakery.feature.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private Long userId;
    private String status; // Или enum OrderStatus, но String проще для JSON
    private BigDecimal totalAmount; // Исправлено на BigDecimal
    private String shippingAddress;
    private String phone;
    private String email;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}