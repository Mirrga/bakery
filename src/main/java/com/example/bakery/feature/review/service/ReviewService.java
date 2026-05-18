package com.example.bakery.feature.review.service;

import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.ProductRepository;
import com.example.bakery.feature.review.dto.ReviewDto;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.entity.Review;
import com.example.bakery.feature.review.repository.ReviewRepository;
import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Создать отзыв о товаре
     */
    public ReviewDto createReview(ReviewRequestDto dto, String username) {
        // Получаем пользователя
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        // Получаем товар
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + dto.getProductId()));

        // Проверяем валидность рейтинга
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("Рейтинг должен быть от 1 до 5");
        }

        // Создаем отзыв
        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);

        return mapToDto(savedReview);
    }

    /**
     * Получить отзывы для товара с пагинацией
     */
    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(this::mapToDto);
    }

    /**
     * Получить средний рейтинг товара
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    /**
     * Получить все отзывы пользователя
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getUserReviews(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
        return reviewRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Удалить отзыв (только свой или админ может удалить любой)
     */
    public void deleteReview(Long reviewId, String username, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Отзыв не найден: " + reviewId));

        if (!isAdmin && !review.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Вы можете удалить только свой отзыв");
        }

        reviewRepository.delete(review);
    }

    /**
     * Маппинг Entity -> DTO
     */
    private ReviewDto mapToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}