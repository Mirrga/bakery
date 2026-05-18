package com.example.bakery.feature.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal price; // Исправлено на BigDecimal
    private BigDecimal subtotal; // Исправлено на BigDecimal

    // Метод для автоматического расчета subtotal, если нужно
    public BigDecimal getSubtotal() {
        if (price != null && quantity != null) {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}