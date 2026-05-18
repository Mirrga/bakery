package com.example.bakery.feature.user.service;

import com.example.bakery.feature.user.dto.RegistrationRequest;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.entity.UserRole;
import com.example.bakery.feature.user.repository.RoleRepository;
import com.example.bakery.feature.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Методы для бизнес-логики (используются в Controller) ---

    /**
     * Получить данные пользователя по имени (для отображения в профиле и т.д.)
     */
    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        return convertToDto(user);
    }

    /**
     * Регистрация нового пользователя
     */
   @Transactional
    public UserDto registerUser(RegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email уже зарегистрирован");
        }

        User user = new User();
        user.setFirstName(request.getFirstName()); // Новое поле
        user.setLastName(request.getLastName());   // Новое поле
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setBonusBalance(BigDecimal.ZERO);

        Role userRole = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Роль CUSTOMER не найдена в БД"));
        
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Приватный метод маппинга
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .bonusBalance(user.getBonusBalance())
                // ИСПРАВЛЕНИЕ: Явно вызываем .name() для получения строки из enum UserRole
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name()) 
                        .collect(Collectors.toSet()))
                .build();
    }

    // --- Методы для Spring Security (внутренние) ---

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                authorities
        );
    }
}