package com.example.bakery.feature.product.repository;

import com.example.bakery.feature.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}