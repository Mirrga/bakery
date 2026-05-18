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
    private Boolean available;
    private String imageUrl;
    private Integer stockQuantity;
    private Long categoryId;
    private String categoryName;
    private List<Long> ingredientIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}