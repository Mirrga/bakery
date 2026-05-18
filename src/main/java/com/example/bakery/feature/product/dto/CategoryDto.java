package com.example.bakery.feature.product.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private String slug;
    private List<Long> productIds;
}