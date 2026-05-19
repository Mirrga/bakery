package com.example.bakery.controller;

import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String home(Model model) {
        log.info("Запрос главной страницы");
        
        // Получаем 4 последних товара для показа на главной (например, "Свежее поступление")
        try {
            List<Product> freshProducts = productService.findAll(PageRequest.of(0, 4)).getContent();
            model.addAttribute("freshProducts", freshProducts);
        } catch (Exception e) {
            log.warn("Не удалось загрузить товары для главной страницы", e);
            model.addAttribute("freshProducts", List.of());
        }

        return "home"; // Ищет файл src/main/resources/templates/home.html
    }
}