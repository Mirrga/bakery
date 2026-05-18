package com.example.bakery.feature.product.service;

import com.example.bakery.feature.product.dto.ProductDto;
import com.example.bakery.feature.product.dto.ProductRequestDto;
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.CategoryRepository;
import com.example.bakery.feature.product.repository.ProductRepository;
import com.example.bakery.feature.review.entity.Review;
import com.example.bakery.feature.review.repository.ReviewRepository;
import com.example.bakery.global.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional
public class ProductService {

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

    // --- Public API Methods (возвращают DTO) ---

    @Transactional(readOnly = true)
    public Page<ProductDto> findAllDto(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ProductDto findByIdDto(Long id) {
        // Для DTO нам нужно подгрузить отзывы, иначе mapToDto получит пустой список из-за LAZY
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Инициализируем коллекцию отзывов явно внутри транзакции
        if (product.getReviews() != null) {
            product.getReviews().size(); 
        }
        
        return mapToDto(product);
    }

    public ProductDto createDto(ProductRequestDto dto) {
        Product created = create(dto);
        return mapToDto(created);
    }

    public ProductDto updateDto(Long id, ProductRequestDto dto) {
        Product updated = update(id, dto);
        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    // --- Internal Logic Methods (работают с Entity) ---

    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public Product create(ProductRequestDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(category);
        // Поля available и stockQuantity удалены, так как их нет в сущности Product
        
        return productRepository.save(product);
    }

    public Product update(Long id, ProductRequestDto dto) {
        Product product = findById(id);
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(category);
        // Поля available и stockQuantity удалены, так как их нет в сущности Product

        return productRepository.save(product);
    }

    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    // --- Mapper Helper ---
    
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
        
        // Безопасное извлечение ID отзывов
        if (product.getReviews() != null) {
            dto.setReviewIds(product.getReviews().stream()
                    .map(Review::getId)
                    .toList());
        } else {
            dto.setReviewIds(Collections.emptyList());
        }
        
        return dto;
    }
}