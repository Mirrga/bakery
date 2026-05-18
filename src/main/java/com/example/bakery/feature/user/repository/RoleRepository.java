package com.example.bakery.feature.user.repository;

import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
}