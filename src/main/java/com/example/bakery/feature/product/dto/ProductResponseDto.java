package com.example.bakery.feature.product.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Специализированный DTO для ответа клиенту (Frontend).
 * Содержит готовые к отображению данные (форматированная цена, дата).
 * Используется в REST API и AJAX ответах.
 */
@Data
@Builder
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    
    // Числовое значение для логики (сортировка, расчеты)
    private BigDecimal priceNumeric;
    
    // Отформатированная строка для вывода на экран (например, "1 250,00 ₽")
    private String priceFormatted;
    
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    
    // Количество отзывов (вместо передачи всего списка ID)
    private int reviewCount;
    
    // Средний рейтинг (если используется)
    private Double averageRating;

    private String createdAtFormatted;
    private String updatedAtFormatted;

    /**
     * Статический метод-конвертер из Entity в ResponseDto
     */
    public static ProductResponseDto fromEntity(com.example.bakery.feature.product.entity.Product product) {
        if (product == null) {
            return null;
        }

        // Форматируем цену
        String priceStr = String.format(new Locale("ru", "RU"), "%,.2f ₽", product.getPrice());
        
    

        // Считаем отзывы безопасно
        int reviewCount = (product.getReviews() != null) ? product.getReviews().size() : 0;

        // Считаем средний рейтинг (упрощенно, если нужно точно - лучше вынести в сервис)
        double avgRating = 0.0;
        if (reviewCount > 0 && product.getReviews() != null) {
             avgRating = product.getReviews().stream()
                .mapToDouble(r -> r.getRating() != null ? r.getRating() : 0.0)
                .average()
                .orElse(0.0);
        }

        Long categoryId = (product.getCategory() != null) ? product.getCategory().getId() : null;
        String categoryName = (product.getCategory() != null) ? product.getCategory().getName() : "Без категории";

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .priceNumeric(product.getPrice())
                .priceFormatted(priceStr)
                .imageUrl(product.getImageUrl())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .reviewCount(reviewCount)
                .averageRating(avgRating)
                .build();
    }
}