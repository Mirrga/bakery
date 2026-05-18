package com.example.bakery.feature.product.repository;

import com.example.bakery.feature.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. Стандартная пагинация + сортировка по цене (возрастание)
    Page<Product> findAllByOrderByPriceAsc(Pageable pageable);

    // 2. Кастомный JPQL запрос: Поиск товаров дешевле указанной цены
    @Query("SELECT p FROM Product p WHERE p.price <= :maxPrice ORDER BY p.price ASC")
    List<Product> findCheapProducts(@Param("maxPrice") BigDecimal maxPrice);

    // 3. Поиск по категории с пагинацией
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    
    // 4. Поиск по названию (частичное совпадение) с пагинацией
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}