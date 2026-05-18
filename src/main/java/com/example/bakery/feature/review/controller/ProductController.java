package com.example.bakery.feature.review.controller;

import com.example.bakery.feature.product.dto.ProductDto;
import com.example.bakery.feature.product.dto.ProductRequestDto;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.service.ProductService;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.service.ReviewService;
import com.example.bakery.global.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductController {

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
        // Для VIEW используем Entity (так удобнее в Thymeleaf)
        Page<Product> page = productService.findAll(pageable);
        model.addAttribute("products", page);
        return "products/list";
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id, Model model, Authentication authentication) {
        Product product = productService.findById(id);
        Double avgRating = productService.getAverageRating(id);

        model.addAttribute("product", product);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewRequestDto", new ReviewRequestDto());

        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("username", authentication.getName());
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        return "products/view";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("productDto", new ProductRequestDto());
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        ProductRequestDto dto = new ProductRequestDto();
        
        // Маппинг Entity -> Request DTO только существующих полей
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        // Поля available/stockQuantity убраны, так как их нет в сущности
        
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
        // Используем метод, возвращающий DTO
        return ResponseEntity.ok(productService.findAllDto(pageable));
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ProductDto> getProductApi(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.findByIdDto(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createProductApi(@Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            ProductDto created = productService.createDto(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера: " + e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateProductApi(@PathVariable Long id, @Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            ProductDto updated = productService.updateDto(id, dto);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка сервера: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteProductApi(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}