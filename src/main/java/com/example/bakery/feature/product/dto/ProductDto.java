package com.example.bakery.feature.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductDto(
        Long id,

        @NotBlank(message = "Название товара обязательно")
        @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
        String name,

        @Size(max = 500, message = "Описание слишком длинное")
        String description,

        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
        BigDecimal price,

        String imageUrl,

        @NotNull(message = "Категория обязательна")
        Long categoryId,
        
        String categoryName // Только для отображения в списке
) {}