package com.example.bakery.feature.product.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private String slug;
    
    // Внимание: Это поле не должно заполняться автоматически из Lazy-коллекции!
    // Заполняйте его вручную в сервисе только если действительно нужно.
    private List<Long> productIds; 
}