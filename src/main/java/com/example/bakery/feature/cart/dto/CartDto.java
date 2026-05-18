package com.example.bakery.feature.cart.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartDto {
    private Long id;
    private String sessionId;
    private Long userId;
    private String userName;
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
}