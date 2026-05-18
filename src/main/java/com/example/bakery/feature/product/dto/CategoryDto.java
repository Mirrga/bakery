package com.example.bakery.feature.product.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryDto(
        Long id,
        @NotBlank(message = "Название категории обязательно")
        String name
) {}