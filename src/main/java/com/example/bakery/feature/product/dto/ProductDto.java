package com.example.bakery.feature.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    
    // Вместо передачи всего списка объектов Review, передаем только ID или краткую информацию
    // Или просто количество отзывов, если детали не нужны в списке
    private Long categoryId;
    private String categoryName;
    private List<Long> reviewIds; // Или Integer reviewCount;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}