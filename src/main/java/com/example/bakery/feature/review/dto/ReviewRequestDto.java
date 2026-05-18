package com.example.bakery.feature.review.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDto {
    
    @NotNull(message = "ID продукта обязательно")
    private Long productId;
    
    @NotNull(message = "Оценка обязательна")
    @Min(value = 1, message = "Оценка должна быть не менее 1")
    @Max(value = 5, message = "Оценка должна быть не более 5")
    private Integer rating; // 1-5
    
    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 5, max = 1000, message = "Комментарий должен содержать от 5 до 1000 символов")
    private String comment;
}