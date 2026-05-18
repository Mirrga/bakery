package com.example.bakery.config;

import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.entity.UserRole;
import com.example.bakery.feature.user.repository.UserRepository;
import com.example.bakery.feature.user.repository.RoleRepository;
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.CategoryRepository;
import com.example.bakery.feature.product.repository.ProductRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, пуста ли БД, чтобы не дублировать данные при каждом перезапуске
        if (categoryRepository.count() == 0) {
            
            // 1. Создаем категории
            Category bread = new Category();
            bread.setName("Хлеб");
            categoryRepository.save(bread);

            Category cakes = new Category();
            cakes.setName("Торты");
            categoryRepository.save(cakes);

            // 2. Создаем товары
            Product borodinsky = new Product();
            borodinsky.setName("Бородинский хлеб");
            borodinsky.setDescription("Классический ржаной хлеб с кориандром");
            borodinsky.setPrice(new BigDecimal("85.00"));
            borodinsky.setCategory(bread);
            productRepository.save(borodinsky);

            Product napoleon = new Product();
            napoleon.setName("Торт Наполеон");
            napoleon.setDescription("Слоеный торт с заварным кремом");
            napoleon.setPrice(new BigDecimal("1200.00"));
            napoleon.setCategory(cakes);
            productRepository.save(napoleon);

            System.out.println("✅ Тестовые данные успешно добавлены в БД!");
        }
    }

}