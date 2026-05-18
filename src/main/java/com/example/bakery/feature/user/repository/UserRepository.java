package com.example.bakery.feature.user.repository;

import com.example.bakery.feature.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);

    // Пример JPQL запроса (требование проекта)
    @Query("SELECT u FROM User u WHERE u.bonusBalance > :minBalance AND u.enabled = true")
    java.util.List<User> findActiveUsersWithMinBonus(@Param("minBalance") java.math.BigDecimal minBalance);
}