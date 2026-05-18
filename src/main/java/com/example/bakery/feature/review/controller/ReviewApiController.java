package com.example.bakery.feature.review.controller;

import com.example.bakery.feature.review.dto.ReviewDto;
import com.example.bakery.feature.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public List<ReviewDto> getProductReviews(@PathVariable Long productId) {
        var page = reviewService.getReviewsByProduct(
            productId,
            PageRequest.of(0, 10, Sort.by("createdAt").descending())
        );
        return page.getContent();
    }

    @GetMapping("/product/{productId}/average")
    public Double getProductAverageRating(@PathVariable Long productId) {
        return reviewService.getAverageRating(productId);
    }
}