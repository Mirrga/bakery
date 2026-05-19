package com.example.bakery.feature.product.controller;

import com.example.bakery.feature.product.dto.ProductDto;
import com.example.bakery.feature.product.dto.ProductRequestDto;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.service.ProductService;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.service.ReviewService;
import com.example.bakery.global.exception.ResourceNotFoundException;
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ReviewService reviewService;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductService productService, ReviewService reviewService, CategoryRepository categoryRepository) {
        this.productService = productService;
        this.reviewService = reviewService;
        this.categoryRepository = categoryRepository;
    }

    // =======================
    // MVC Views (Thymeleaf) - Работаем с Entity для удобства шаблонов
    // =======================

    @GetMapping
    public String listProducts(Model model, @PageableDefault(size = 8) Pageable pageable) {
        log.debug("Запрос списка товаров, страница: {}, размер: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Product> page = productService.findAll(pageable);
        log.debug("Найдено товаров на странице: {}", page.getNumberOfElements());
        model.addAttribute("products", page);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id, Model model, Authentication authentication) {
        log.debug("Запрос страницы товара с ID: {}", id);
        
        Product product = productService.findById(id);
        Double avgRating = productService.getAverageRating(id);

        model.addAttribute("product", product);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewRequestDto", new ReviewRequestDto());

        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("username", authentication.getName());
            log.debug("Пользователь {} просматривает товар {}", authentication.getName(), id);
        } else {
            model.addAttribute("isLoggedIn", false);
            log.debug("Анонимный пользователь просматривает товар {}", id);
        }

        return "products/view";
    }

    /**
     * Форма создания товара - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        log.debug("Запрос формы создания нового товара (ADMIN)");
        model.addAttribute("productDto", new ProductRequestDto());
        model.addAttribute("categories", categoryRepository.findAll()); // Передаем категории
        return "products/form";
    }

    /**
     * Форма редактирования товара - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.debug("Запрос формы редактирования товара с ID: {} (ADMIN)", id);
        Product product = productService.findById(id);
        ProductRequestDto dto = new ProductRequestDto();
        
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
        }

        model.addAttribute("productDto", dto);
        model.addAttribute("productId", id);
        model.addAttribute("categories", categoryRepository.findAll()); // Передаем категории
        return "products/form";
    }

    // =======================
    // REST API - Работаем строго с DTO
    // =======================

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Page<ProductDto>> getProductsApi(@PageableDefault(size = 10) Pageable pageable) {
        log.debug("REST API: Запрос списка товаров (JSON), страница: {}", pageable.getPageNumber());
        Page<ProductDto> result = productService.findAllDto(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ProductDto> getProductApi(@PathVariable Long id) {
        log.debug("REST API: Запрос товара с ID: {}", id);
        try {
            ProductDto dto = productService.findByIdDto(id);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Товар с ID {} не найден", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Создание товара через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @PostMapping("/api")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> createProductApi(@Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        log.info("REST API: Попытка создания нового товара: {} (ADMIN)", dto.getName());
        
        if (result.hasErrors()) {
            log.warn("REST API: Ошибка валидации при создании товара: {}", result.getAllErrors());
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            ProductDto created = productService.createDto(dto);
            log.info("REST API: Товар успешно создан с ID: {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Категория не найдена при создании товара: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("REST API: Неожиданная ошибка при создании товара", e);
            return ResponseEntity.internalServerError().body("Ошибка при создании: " + e.getMessage());
        }
    }

    /**
     * Обновление товара через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @PutMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> updateProductApi(@PathVariable Long id, @Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        log.info("REST API: Попытка обновления товара с ID: {} (ADMIN)", id);
        
        if (result.hasErrors()) {
            log.warn("REST API: Ошибка валидации при обновлении товара {}: {}", id, result.getAllErrors());
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            ProductDto updated = productService.updateDto(id, dto);
            log.info("REST API: Товар успешно обновлен: {}", id);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Товар или категория не найдены при обновлении {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("REST API: Неожиданная ошибка при обновлении товара {}", id, e);
            return ResponseEntity.internalServerError().body("Ошибка при обновлении: " + e.getMessage());
        }
    }

    /**
     * Удаление товара через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Void> deleteProductApi(@PathVariable Long id) {
        log.info("REST API: Попытка удаления товара с ID: {} (ADMIN)", id);
        try {
            productService.delete(id);
            log.info("REST API: Товар успешно удален: {}", id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Товар не найден для удаления {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("REST API: Ошибка при удалении товара {}", id, e);
            throw e; // Пусть глобальный обработчик обработает это
        }
    }
}