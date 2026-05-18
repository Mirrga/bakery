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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден: " + dto.getProductId()));

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("Рейтинг должен быть от 1 до 5");
        }

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
     * Примечание: Для полной оптимизации лучше добавить метод в репозиторий с JOIN FETCH
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
        
        // Здесь также может возникнуть N+1, если отзывов много. 
        // Для продакшена лучше использовать @EntityGraph или отдельный JPQL запрос с fetch.
        return reviewRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Удалить отзыв
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
     * Исправлено: userName вместо username
     * Добавлена проверка на null для безопасности
     */
    private ReviewDto mapToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        
        // Безопасное извлечение данных о продукте
        if (review.getProduct() != null) {
            dto.setProductId(review.getProduct().getId());
            dto.setProductName(review.getProduct().getName());
        }
        
        // Безопасное извлечение данных о пользователе
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
            dto.setUserName(review.getUser().getUsername()); // Исправлено на setUserName
        }
        
        // Поле approved пока всегда true, так как в сущности его нет
        dto.setApproved(true); 

        return dto;
    }
}