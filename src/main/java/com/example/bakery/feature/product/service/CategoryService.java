package com.example.bakery.feature.product.service;

import com.example.bakery.feature.product.dto.CategoryDto;
import com.example.bakery.feature.product.dto.CategoryRequestDto;
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.entity.Product; // Добавлен импорт
import com.example.bakery.feature.product.repository.CategoryRepository;
import com.example.bakery.global.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // --- API Methods (DTO) ---

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToDto(category);
    }

    public CategoryDto create(CategoryRequestDto dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        
        if (!StringUtils.hasText(dto.getSlug())) {
            category.setSlug(generateSlug(dto.getName()));
        } else {
            category.setSlug(dto.getSlug());
        }

        return mapToDto(categoryRepository.save(category));
    }

    public CategoryDto update(Long id, CategoryRequestDto dto) {
        Category category = findByIdEntity(id);
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        
        if (StringUtils.hasText(dto.getSlug())) {
            category.setSlug(dto.getSlug());
        }

        return mapToDto(categoryRepository.save(category));
    }

    public void delete(Long id) {
        Category category = findByIdEntity(id);
        categoryRepository.delete(category);
    }

    // --- Helper Methods ---

    private Category findByIdEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryDto mapToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSlug(category.getSlug());
        
        // Безопасная работа с ленивой загрузкой
        if (category.getProducts() != null) {
            // Проверяем, инициализирована ли коллекция, чтобы избежать LazyInitializationException
            if (org.hibernate.Hibernate.isInitialized(category.getProducts())) {
                dto.setProductIds(category.getProducts().stream()
                    .map(Product::getId)
                    .collect(Collectors.toList()));
            } else {
                // Если не инициализирована, оставляем пустой список или null
                dto.setProductIds(List.of());
            }
        }
        
        return dto;
    }

    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-zа-яё0-9]", "-")
                .replaceAll("-+", "-");
    }
}