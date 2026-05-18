package com.example.bakery.feature.review.service;

import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.ProductRepository;
import com.example.bakery.feature.review.dto.ReviewDto;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.entity.Review;
import com.example.bakery.feature.review.repository.ReviewRepository;
import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.entity.UserRole;
import com.example.bakery.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Создать отзыв о товаре
     * Доступно только авторизованным пользователям (проверка имени пользователя)
     */
    public ReviewDto createReview(ReviewRequestDto dto, String username) {
        log.info("Создание отзыва для товара ID={} пользователем {}", dto.getProductId(), username);

        // Проверка: если username null или пустой, значит пользователь не авторизован
        if (username == null || username.isEmpty()) {
            log.warn("Попытка создания отзыва неавторизованным пользователем");
            throw new RuntimeException("Для создания отзыва необходимо войти в систему");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь {} не найден при создании отзыва", username);
                    return new RuntimeException("Пользователь не найден: " + username);
                });

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> {
                    log.warn("Товар ID={} не найден при создании отзыва", dto.getProductId());
                    return new RuntimeException("Товар не найден: " + dto.getProductId());
                });

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            log.warn("Некорректный рейтинг {} для отзыва от пользователя {}", dto.getRating(), username);
            throw new RuntimeException("Рейтинг должен быть от 1 до 5");
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Отзыв успешно создан с ID={}", savedReview.getId());

        return mapToDto(savedReview);
    }

    /**
     * Получить отзывы для товара с пагинацией
     * Публичный метод
     */
    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByProduct(Long productId, Pageable pageable) {
        log.debug("Запрос отзывов для товара ID={}, страница={}", productId, pageable.getPageNumber());
        Page<ReviewDto> result = reviewRepository.findByProductId(productId, pageable)
                .map(this::mapToDto);
        log.debug("Найдено отзывов: {}", result.getTotalElements());
        return result;
    }

    /**
     * Получить средний рейтинг товара
     * Публичный метод
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        log.debug("Запрос среднего рейтинга для товара ID={}", productId);
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        double result = avg != null ? avg : 0.0;
        log.debug("Средний рейтинг: {}", result);
        return result;
    }

    /**
     * Получить все отзывы пользователя
     * Доступно только самому пользователю
     */
    @Transactional(readOnly = true)
    public List<ReviewDto> getUserReviews(String username) {
        log.debug("Запрос отзывов пользователя {}", username);
        
        // Проверка текущего авторизованного пользователя
        String currentUsername = getCurrentUsername();
        if (currentUsername == null || !currentUsername.equals(username)) {
            log.warn("Пользователь {} попытался получить отзывы другого пользователя {}", currentUsername, username);
            throw new RuntimeException("Доступ запрещен. Вы можете просматривать только свои отзывы.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь {} не найден при запросе отзывов", username);
                    return new RuntimeException("Пользователь не найден: " + username);
                });
        
        List<ReviewDto> reviews = reviewRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDto)
                .toList();
        
        log.info("Получено {} отзывов для пользователя {}", reviews.size(), username);
        return reviews;
    }

    /**
     * Удалить отзыв
     * Доступно владельцу отзыва или администратору
     */
    public void deleteReview(Long reviewId, String username, boolean isAdmin) {
        log.info("Попытка удаления отзыва ID={} пользователем {} (admin={})", reviewId, username, isAdmin);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("Отзыв ID={} не найден при попытке удаления", reviewId);
                    return new RuntimeException("Отзыв не найден: " + reviewId);
                });

        // Проверка прав доступа
        boolean isOwner = review.getUser().getUsername().equals(username);
        
        if (!isAdmin && !isOwner) {
            log.warn("Пользователь {} попытался удалить чужой отзыв ID={}. Владелец: {}", 
                     username, reviewId, review.getUser().getUsername());
            throw new RuntimeException("Вы можете удалить только свой отзыв");
        }

        reviewRepository.delete(review);
        log.info("Отзыв ID={} успешно удален пользователем {}", reviewId, username);
    }

    /**
     * Вспомогательный метод для получения имени текущего пользователя из SecurityContext
     */
    private String getCurrentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Вспомогательный метод для проверки роли ADMIN у текущего пользователя
     */
    private boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Маппинг Entity -> DTO
     */
    private ReviewDto mapToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        
        if (review.getProduct() != null) {
            dto.setProductId(review.getProduct().getId());
            dto.setProductName(review.getProduct().getName());
        } else {
            log.warn("У отзыва ID={} отсутствует связь с продуктом", review.getId());
        }
        
        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
            dto.setUserName(review.getUser().getUsername());
        } else {
            log.warn("У отзыва ID={} отсутствует связь с пользователем", review.getId());
        }
        
        dto.setApproved(true); 

        return dto;
    }
}