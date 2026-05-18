package com.example.bakery.feature.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequestDto {

    @NotBlank(message = "Название продукта обязательно")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
    private BigDecimal price;

    @NotNull(message = "Категория обязательна")
    private Long categoryId;

    // Новые поля для соответствия сущности Product

    @Size(max = 255, message = "URL изображения слишком длинный")
    private String imageUrl;

    private Boolean available = true; // По умолчанию товар доступен

    @NotNull(message = "Количество на складе обязательно")
    @DecimalMin(value = "0", message = "Количество не может быть отрицательным")
    private Integer stockQuantity = 0;
}