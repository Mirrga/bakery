package com.example.bakery.feature.review.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private Boolean approved;
    private LocalDateTime createdAt;
}