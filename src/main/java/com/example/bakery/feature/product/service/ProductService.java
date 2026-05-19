package com.example.bakery.feature.product.service;

import com.example.bakery.feature.product.dto.ProductDto;
import com.example.bakery.feature.product.dto.ProductRequestDto;
import com.example.bakery.feature.product.dto.ProductResponseDto; // Импортируем новый DTO
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.CategoryRepository;
import com.example.bakery.feature.product.repository.ProductRepository;
import com.example.bakery.feature.review.entity.Review;
import com.example.bakery.feature.review.repository.ReviewRepository;
import com.example.bakery.global.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(ProductRepository productRepository, 
                          CategoryRepository categoryRepository, 
                          ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
    }

    // =======================
    // НОВЫЕ МЕТОДЫ ДЛЯ FRONTEND / AJAX (ProductResponseDto)
    // =======================

    /**
     * Возвращает товар в виде ResponseDTO (с форматированной ценой, рейтингом и т.д.)
     * Используется для детального просмотра через REST API.
     */
    @Transactional(readOnly = true)
    public ProductResponseDto findResponseDtoById(Long id) {
        log.debug("Поиск товара по ID (ResponseDTO): {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Товар не найден с ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        
        // Принудительная инициализация отзывов для расчета рейтинга внутри транзакции
        if (product.getReviews() != null) {
            product.getReviews().size(); 
            log.debug("Инициализировано {} отзывов для товара {}", product.getReviews().size(), id);
        }
        
        return ProductResponseDto.fromEntity(product);
    }

    /**
     * Возвращает список товаров в виде ResponseDTO
     */
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> findAllResponseDto(Pageable pageable) {
        log.debug("Запрос списка товаров (ResponseDTO): страница={}, размер={}", pageable.getPageNumber(), pageable.getPageSize());
        // Маппинг происходит внутри транзакции, чтобы ленивая загрузка отзывов сработала корректно при необходимости
        return productRepository.findAll(pageable).map(this::mapToResponseDto);
    }

    // =======================
    // СУЩЕСТВУЮЩИЕ МЕТОДЫ (Без изменений)
    // =======================

    @Transactional(readOnly = true)
    public Page<ProductDto> findAllDto(Pageable pageable) {
        log.debug("Запрос списка товаров (DTO): страница={}, размер={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductDto> result = productRepository.findAll(pageable).map(this::mapToDto);
        log.debug("Найдено товаров: {}", result.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public ProductDto findByIdDto(Long id) {
        log.debug("Поиск товара по ID (DTO): {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Товар не найден с ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        
        if (product.getReviews() != null) {
            product.getReviews().size(); 
            log.debug("Инициализировано {} отзывов для товара {}", product.getReviews().size(), id);
        }
        
        return mapToDto(product);
    }

    public ProductDto createDto(ProductRequestDto dto) {
        log.info("Создание нового товара: {}", dto.getName());
        Product created = create(dto);
        log.info("Товар успешно создан с ID: {}", created.getId());
        return mapToDto(created);
    }

    public ProductDto updateDto(Long id, ProductRequestDto dto) {
        log.info("Обновление товара ID={}: {}", id, dto.getName());
        Product updated = update(id, dto);
        log.info("Товар ID={} успешно обновлен", id);
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        log.debug("Расчет среднего рейтинга для товара ID={}", productId);
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        double result = avg != null ? avg : 0.0;
        log.debug("Средний рейтинг товара {}: {}", productId, result);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        log.debug("Запрос списка товаров (Entity): страница={}, размер={}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        log.debug("Поиск товара по ID (Entity): {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Товар не найден с ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
    }

    public Product create(ProductRequestDto dto) {
        log.debug("Сохранение товара в БД: {}", dto.getName());
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Категория не найдена с ID: {}", dto.getCategoryId());
                    return new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId());
                });

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(category);
        
        Product saved = productRepository.save(product);
        log.debug("Товар сохранен с ID: {}", saved.getId());
        return saved;
    }

    public Product update(Long id, ProductRequestDto dto) {
        log.debug("Поиск товара для обновления: {}", id);
        Product product = findById(id);
        
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Категория не найдена с ID: {}", dto.getCategoryId());
                    return new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId());
                });

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        log.info("Товар ID={} успешно обновлен в БД", id);
        return saved;
    }

    public void delete(Long id) {
        log.info("Удаление товара ID={}", id);
        Product product = findById(id);
        productRepository.delete(product);
        log.info("Товар ID={} успешно удален", id);
    }

    // =======================
    // МАППЕРЫ (Без изменений + новый маппер)
    // =======================
    
    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        
        if (product.getReviews() != null) {
            dto.setReviewIds(product.getReviews().stream()
                    .map(Review::getId)
                    .toList());
        } else {
            dto.setReviewIds(Collections.emptyList());
        }
        
        return dto;
    }

    /**
     * Новый приватный метод для маппинга в ResponseDTO
     */
    private ProductResponseDto mapToResponseDto(Product product) {
        // Делегируем логику статическому методу в DTO классе
        return ProductResponseDto.fromEntity(product);
    }
}