package com.example.bakery.feature.product.controller;

import com.example.bakery.feature.product.dto.CategoryDto;
import com.example.bakery.feature.product.dto.CategoryRequestDto;
import com.example.bakery.feature.product.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // --- MVC Views ---

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "categories/list"; // Создай шаблон categories/list.html
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("categoryDto", new CategoryRequestDto());
        return "categories/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        CategoryDto category = categoryService.findById(id);
        CategoryRequestDto dto = new CategoryRequestDto();
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setSlug(category.getSlug());

        model.addAttribute("categoryDto", dto);
        model.addAttribute("categoryId", id);
        return "categories/form";
    }

    // --- REST API ---

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<CategoryDto>> getCategoriesApi() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<CategoryDto> getCategoryApi(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createCategoryApi(@Valid @RequestBody CategoryRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            CategoryDto created = categoryService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCategoryApi(@PathVariable Long id, @Valid @RequestBody CategoryRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            CategoryDto updated = categoryService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteCategoryApi(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}