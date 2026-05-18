package com.example.bakery.feature.review.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDto {
    private Long productId;
    private Integer rating; // 1-5
    private String comment;
}