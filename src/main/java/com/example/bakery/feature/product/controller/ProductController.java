package com.example.bakery.feature.product.controller;

import com.example.bakery.feature.product.dto.ProductRequestDto;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.service.ProductService;
import com.example.bakery.feature.review.dto.ReviewRequestDto;
import com.example.bakery.feature.review.service.ReviewService;
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

    // --- MVC Views (для Thymeleaf) ---

    @GetMapping
    public String listProducts(Model model, @PageableDefault(size = 8) Pageable pageable) {
        Page<Product> page = productService.findAll(pageable);
        model.addAttribute("products", page);
        return "products/list"; // Шаблон products/list.html
    }

    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id, Model model, Authentication authentication) {
        Product product = productService.findById(id);
        Double avgRating = productService.getAverageRating(id);

        model.addAttribute("product", product);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewRequestDto", new ReviewRequestDto());

        // Если пользователь авторизован, добавляем информацию для формы отзыва
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
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategoryId(product.getCategory().getId());

        model.addAttribute("productDto", dto);
        model.addAttribute("productId", id);
        return "products/form";
    }

    // --- REST API Endpoints ---

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Page<Product>> getProductsApi(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(pageable));
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createProductApi(@Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            Product created = productService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateProductApi(@PathVariable Long id, @Valid @RequestBody ProductRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            Product updated = productService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteProductApi(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}