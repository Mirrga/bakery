package com.example.bakery.feature.review.controller;

import com.example.bakery.feature.review.dto.ReviewDto;
import com.example.bakery.feature.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private static final Logger log = LoggerFactory.getLogger(ReviewApiController.class);

    private final ReviewService reviewService;

    /**
     * Получить отзывы для товара (публичный доступ)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewDto>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Запрос отзывов для продукта ID={}, страница={}, размер={}", productId, page, size);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewDto> result = reviewService.getReviewsByProduct(productId, pageable);
        log.debug("Найдено отзывов: {}", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    /**
     * Получить средний рейтинг товара (публичный доступ)
     */
    @GetMapping("/product/{productId}/average")
    public ResponseEntity<Double> getProductAverageRating(@PathVariable Long productId) {
        log.debug("Запрос среднего рейтинга для продукта ID={}", productId);
        Double average = reviewService.getAverageRating(productId);
        log.debug("Средний рейтинг: {}", average);
        return ResponseEntity.ok(average);
    }
    
    /**
     * Получить мои отзывы. Доступ только авторизованным пользователям.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyReviews(org.springframework.security.core.Authentication authentication) {
        // Дополнительная проверка на всякий случай, хотя @PreAuthorize должен сработать раньше
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Попытка доступа к отзывам без авторизации (защита дублирована)");
            return ResponseEntity.status(401).body("Требуется авторизация");
        }
        
        String username = authentication.getName();
        log.info("Запрос отзывов пользователя: {}", username);
        
        try {
            var reviews = reviewService.getUserReviews(username);
            log.debug("Найдено отзывов для пользователя {}: {}", username, reviews.size());
            return ResponseEntity.ok(reviews);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении отзывов пользователя {}", username, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}