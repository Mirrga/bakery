package com.example.bakery.feature.product.service;

import com.example.bakery.feature.product.dto.CategoryDto;
import com.example.bakery.feature.product.dto.CategoryRequestDto;
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.CategoryRepository;
import com.example.bakery.global.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // --- API Methods (DTO) ---

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        log.debug("Запрос списка всех категорий");
        List<CategoryDto> result = categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        log.debug("Найдено категорий: {}", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public CategoryDto findById(Long id) {
        log.debug("Поиск категории по ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Категория не найдена с ID: {}", id);
                    return new ResourceNotFoundException("Category not found with id: " + id);
                });
        log.debug("Категория найдена: {}", category.getName());
        return mapToDto(category);
    }

    public CategoryDto create(CategoryRequestDto dto) {
        log.info("Создание новой категории: {}", dto.getName());
        
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        
        if (!StringUtils.hasText(dto.getSlug())) {
            String slug = generateSlug(dto.getName());
            log.debug("Сгенерирован slug: {}", slug);
            category.setSlug(slug);
        } else {
            category.setSlug(dto.getSlug());
        }

        Category saved = categoryRepository.save(category);
        log.info("Категория успешно создана с ID: {}", saved.getId());
        return mapToDto(saved);
    }

    public CategoryDto update(Long id, CategoryRequestDto dto) {
        log.info("Обновление категории ID: {}", id);
        
        Category category = findByIdEntity(id);
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        
        if (StringUtils.hasText(dto.getSlug())) {
            category.setSlug(dto.getSlug());
            log.debug("Slug обновлен на: {}", dto.getSlug());
        }

        Category updated = categoryRepository.save(category);
        log.info("Категория ID {} успешно обновлена", updated.getId());
        return mapToDto(updated);
    }

    public void delete(Long id) {
        log.info("Удаление категории ID: {}", id);
        Category category = findByIdEntity(id);
        
        // Проверка на наличие товаров (опционально, но полезно для логов)
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
             log.warn("Попытка удаления категории {}, которая содержит товары. Количество: {}", 
                      category.getName(), category.getProducts().size());
        }
        
        categoryRepository.delete(category);
        log.info("Категория ID {} успешно удалена", id);
    }

    // --- Helper Methods ---

    private Category findByIdEntity(Long id) {
        log.debug("Поиск сущности категории по ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Сущность категории не найдена с ID: {}", id);
                    return new ResourceNotFoundException("Category not found with id: " + id);
                });
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
                List<Long> productIds = category.getProducts().stream()
                    .map(Product::getId)
                    .collect(Collectors.toList());
                dto.setProductIds(productIds);
                log.debug("Для категории {} загружено ID продуктов: {}", category.getName(), productIds.size());
            } else {
                // Если не инициализирована, оставляем пустой список
                dto.setProductIds(List.of());
                log.debug("Коллекция продуктов для категории {} не инициализирована", category.getName());
            }
        }
        
        return dto;
    }

    private String generateSlug(String name) {
        if (name == null) {
            return "";
        }
        String slug = name.toLowerCase()
                .replaceAll("[^a-zа-яё0-9]", "-")
                .replaceAll("-+", "-");
        log.trace("Генерация slug из '{}': '{}'", name, slug);
        return slug;
    }
}