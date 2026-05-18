package com.example.bakery.feature.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private Long userId;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String phone;
    private String email;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}