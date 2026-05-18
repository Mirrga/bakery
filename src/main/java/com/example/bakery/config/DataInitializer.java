package com.example.bakery.config;

import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.UserRole;
import com.example.bakery.feature.user.repository.UserRepository;
import com.example.bakery.feature.user.repository.RoleRepository;
import com.example.bakery.feature.product.entity.Category;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.CategoryRepository;
import com.example.bakery.feature.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeAdmin();
        initializeProducts();
    }

    /**
     * Инициализация ролей (ADMIN, CUSTOMER)
     */
    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            log.info("Инициализация ролей...");
            
            Role adminRole = new Role();
            adminRole.setName(UserRole.ADMIN);
            roleRepository.save(adminRole);

            Role customerRole = new Role();
            customerRole.setName(UserRole.CUSTOMER);
            roleRepository.save(customerRole);
            
            log.info("✅ Роли ADMIN и CUSTOMER созданы");
        } else {
            log.debug("Роли уже существуют в БД (всего: {})", roleRepository.count());
        }
    }

    /**
     * Инициализация пользователя Admin
     */
    private void initializeAdmin() {
        String adminUsername = "admin";
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            log.info("Создание пользователя Admin...");
            
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@bakery.com");
            admin.setFirstName("Admin");
            admin.setLastName("Adminov");
            admin.setPassword(passwordEncoder.encode("admin123")); // Пароль: admin123
            admin.setEnabled(true);
            admin.setBonusBalance(BigDecimal.ZERO);

            // Назначаем роль ADMIN
            Role adminRole = roleRepository.findByName(UserRole.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Роль ADMIN не найдена! Запустите сначала initializeRoles."));
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);
            log.info("✅ Пользователь Admin создан (login: admin, password: admin123)");
        } else {
            log.debug("Пользователь Admin уже существует");
        }
    }

    /**
     * Инициализация категорий и товаров
     */
    private void initializeProducts() {
        if (categoryRepository.count() == 0) {
            log.info("Инициализация категорий и товаров...");
            
            // 1. Создаем категории
            Category bread = new Category();
            bread.setName("Хлеб");
            categoryRepository.save(bread);

            Category cakes = new Category();
            cakes.setName("Торты");
            categoryRepository.save(cakes);

            Category pastries = new Category();
            pastries.setName("Пирожные");
            categoryRepository.save(pastries);

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
            
            Product eclair = new Product();
            eclair.setName("Эклер ванильный");
            eclair.setDescription("Заварное пирожное с ванильным кремом");
            eclair.setPrice(new BigDecimal("150.00"));
            eclair.setCategory(pastries);
            productRepository.save(eclair);

            log.info("✅ Категории и товары успешно добавлены в БД");
        } else {
            log.debug("Категории уже существуют в БД (всего: {})", categoryRepository.count());
        }
    }
}