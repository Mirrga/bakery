package com.example.bakery.config;

import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.UserRole;
import com.example.bakery.feature.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (UserRole userRole : UserRole.values()) {
            roleRepository.findByName(userRole).orElseGet(() -> {
                return roleRepository.save(new Role(userRole));
            });
        }
    }
}