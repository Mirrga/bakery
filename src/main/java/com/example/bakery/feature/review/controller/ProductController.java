package com.example.bakery.feature.review.controller;

import com.example.bakery.feature.product.dto.ProductDto;
import com.example.bakery.feature.product.dto.ProductRequestDto;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.service.ProductService;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.service.ReviewService;
import com.example.bakery.global.exception.ResourceNotFoundException;
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

@Controller
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    // ==========================================
    // MVC CONTROLLER (Для Thymeleaf шаблонов)
    // ==========================================

    @GetMapping
    public String listProducts(Model model, @PageableDefault(size = 8) Pageable pageable) {
        log.debug("Запрос списка товаров, страница={}, размер={}", pageable.getPageNumber(), pageable.getPageSize());
        // Для VIEW используем Entity (так удобнее в Thymeleaf)
        Page<Product> page = productService.findAll(pageable);
        log.debug("Найдено {} товаров на странице", page.getNumberOfElements());
        model.addAttribute("products", page);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id, Model model, Authentication authentication) {
        log.debug("Запрос деталей товара с ID={}", id);
        
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
        log.debug("Запрос формы создания товара (требуется ADMIN)");
        model.addAttribute("productDto", new ProductRequestDto());
        return "products/form";
    }

    /**
     * Форма редактирования товара - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.debug("Запрос формы редактирования товара с ID={} (требуется ADMIN)", id);
        
        Product product = productService.findById(id);
        ProductRequestDto dto = new ProductRequestDto();
        
        // Маппинг Entity -> Request DTO только существующих полей
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
        }

        model.addAttribute("productDto", dto);
        model.addAttribute("productId", id);
        return "products/form";
    }

    // ==========================================
    // REST API CONTROLLER (Возвращает JSON + DTO)
    // ==========================================

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Page<ProductDto>> getProductsApi(@PageableDefault(size = 10) Pageable pageable) {
        log.debug("REST API: Запрос списка товаров (JSON), страница={}, размер={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductDto> result = productService.findAllDto(pageable);
        log.debug("REST API: Найдено {} товаров (DTO)", result.getNumberOfElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ProductDto> getProductApi(@PathVariable Long id) {
        log.debug("REST API: Запрос товара с ID={}", id);
        try {
            ProductDto dto = productService.findByIdDto(id);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Товар с ID={} не найден", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Создание товара через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @PostMapping("/api")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> createProductApi(@Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        log.info("REST API: Попытка создания товара (требуется ADMIN): {}", dto.getName());
        
        if (result.hasErrors()) {
            log.warn("REST API: Ошибка валидации при создании товара: {}", result.getAllErrors());
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            ProductDto created = productService.createDto(dto);
            log.info("REST API: Товар успешно создан с ID={}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Категория не найдена при создании товара: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("REST API: Внутренняя ошибка при создании товара", e);
            return ResponseEntity.internalServerError().body("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Обновление товара через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @PutMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> updateProductApi(@PathVariable Long id, @Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        log.info("REST API: Попытка обновления товара с ID={} (требуется ADMIN)", id);
        
        if (result.hasErrors()) {
            log.warn("REST API: Ошибка валидации при обновлении товара ID={}: {}", id, result.getAllErrors());
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            ProductDto updated = productService.updateDto(id, dto);
            log.info("REST API: Товар успешно обновлен с ID={}", id);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Ресурс не найден при обновлении товара ID={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("REST API: Внутренняя ошибка при обновлении товара ID={}", id, e);
            return ResponseEntity.internalServerError().body("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Удаление товара через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Void> deleteProductApi(@PathVariable Long id) {
        log.info("REST API: Попытка удаления товара с ID={} (требуется ADMIN)", id);
        try {
            productService.delete(id);
            log.info("REST API: Товар успешно удален с ID={}", id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("REST API: Товар не найден для удаления с ID={}", id);
            return ResponseEntity.notFound().build();
        }
    }
}