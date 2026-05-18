package com.example.bakery.feature.product.controller;

import com.example.bakery.feature.product.dto.CategoryDto;
import com.example.bakery.feature.product.dto.CategoryRequestDto;
import com.example.bakery.feature.product.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // --- MVC Views ---

    @GetMapping
    public String listCategories(Model model) {
        log.debug("Запрос списка всех категорий");
        List<CategoryDto> categories = categoryService.findAll();
        log.debug("Найдено категорий: {}", categories.size());
        model.addAttribute("categories", categories);
        return "categories/list";
    }

    /**
     * Форма создания категории - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        log.debug("Запрос формы создания категории (ADMIN)");
        model.addAttribute("categoryDto", new CategoryRequestDto());
        return "categories/form";
    }

    /**
     * Форма редактирования категории - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.debug("Запрос формы редактирования категории с ID: {} (ADMIN)", id);
        try {
            CategoryDto category = categoryService.findById(id);
            CategoryRequestDto dto = new CategoryRequestDto();
            dto.setName(category.getName());
            dto.setDescription(category.getDescription());
            dto.setSlug(category.getSlug());

            model.addAttribute("categoryDto", dto);
            model.addAttribute("categoryId", id);
            return "categories/form";
        } catch (Exception e) {
            log.warn("Категория с ID {} не найдена для редактирования: {}", id, e.getMessage());
            model.addAttribute("error", "Категория не найдена");
            return "redirect:/categories";
        }
    }

    // --- REST API ---

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<CategoryDto>> getCategoriesApi() {
        log.debug("API запрос: получение списка всех категорий");
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<CategoryDto> getCategoryApi(@PathVariable Long id) {
        log.debug("API запрос: получение категории с ID {}", id);
        try {
            return ResponseEntity.ok(categoryService.findById(id));
        } catch (Exception e) {
            log.warn("API: Категория с ID {} не найдена", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Создание категории через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @PostMapping("/api")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> createCategoryApi(@Valid @RequestBody CategoryRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            log.warn("API: Ошибка валидации при создании категории: {}", result.getAllErrors());
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            log.info("API: Создание новой категории: {} (ADMIN)", dto.getName());
            CategoryDto created = categoryService.create(dto);
            log.info("API: Категория успешно создана с ID: {}", created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("API: Ошибка при создании категории", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Обновление категории через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @PutMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> updateCategoryApi(@PathVariable Long id, @Valid @RequestBody CategoryRequestDto dto, BindingResult result) {
        if (result.hasErrors()) {
            log.warn("API: Ошибка валидации при обновлении категории {}: {}", id, result.getAllErrors());
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        try {
            log.info("API: Обновление категории с ID {} (ADMIN)", id);
            CategoryDto updated = categoryService.update(id, dto);
            log.info("API: Категория с ID {} успешно обновлена", id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("API: Ошибка при обновлении категории с ID {}", id, e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Удаление категории через API - ТОЛЬКО ДЛЯ АДМИНИСТРАТОРОВ
     */
    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Void> deleteCategoryApi(@PathVariable Long id) {
        try {
            log.info("API: Удаление категории с ID {} (ADMIN)", id);
            categoryService.delete(id);
            log.info("API: Категория с ID {} успешно удалена", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("API: Ошибка при удалении категории с ID {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}