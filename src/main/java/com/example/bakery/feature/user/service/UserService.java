package com.example.bakery.feature.user.service;

import com.example.bakery.feature.auth.dto.RegistrationRequest;
import com.example.bakery.feature.auth.service.FormTokenService;
import com.example.bakery.feature.user.dto.UserDto;
import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.entity.UserRole;
import com.example.bakery.feature.user.repository.RoleRepository;
import com.example.bakery.feature.user.repository.UserRepository;
import com.example.bakery.global.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FormTokenService formTokenService;

    // --- Методы для бизнес-логики ---

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.debug("Поиск пользователя по ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден с ID: {}", id);
                    return new ResourceNotFoundException("Пользователь не найден с id: " + id);
                });
        log.debug("Пользователь найден: {} {}", user.getFirstName(), user.getLastName());
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByUsername(String username) {
        log.debug("Поиск пользователя по username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", username);
                    return new UsernameNotFoundException("Пользователь не найден: " + username);
                });
        return convertToDto(user);
    }

    /**
     * Регистрация нового пользователя с проверкой токена защиты от CSRF/повторной отправки
     */
    @Transactional
    public UserDto registerUser(RegistrationRequest request, String token, String sessionId) {
        log.info("=== НАЧАЛО РЕГИСТРАЦИИ === Пользователь: {}, Email: {}", request.getUsername(), request.getEmail());

        // 1. Проверка и инвалидация токена
        log.debug("Валидация токена безопасности...");
        boolean isTokenValid = formTokenService.validateAndInvalidateToken(token, sessionId);
        
        if (!isTokenValid) {
            log.error("ОШИБКА: Токен безопасности невалиден или уже использован для сессии {}", sessionId);
            throw new IllegalArgumentException("Неверный токен безопасности формы. Обновите страницу и попробуйте снова.");
        }
        log.debug("Токен успешно валидирован.");

        // 2. Валидация паролей
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Ошибка регистрации: пароли не совпадают для пользователя {}", request.getEmail());
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        // 3. Проверка уникальности username
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Ошибка регистрации: имя пользователя '{}' уже занято", request.getUsername());
            throw new IllegalArgumentException("Пользователь с таким именем уже существует");
        }

        // 4. Проверка уникальности email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Ошибка регистрации: email '{}' уже зарегистрирован", request.getEmail());
            throw new IllegalArgumentException("Email уже зарегистрирован");
        }

        // 5. Создание пользователя
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        
        String rawPassword = request.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        log.debug("Пароль захеширован: {} -> {}", rawPassword.length(), encodedPassword.length());
        
        user.setPassword(encodedPassword);
        user.setEnabled(true);
        user.setBonusBalance(BigDecimal.ZERO);

        // 6. Назначение роли
        Role userRole = roleRepository.findByName(UserRole.CUSTOMER)
                .orElseThrow(() -> {
                    log.error("=== КРИТИЧЕСКАЯ ОШИБКА === Роль CUSTOMER не найдена в БД!");
                    return new RuntimeException("Роль CUSTOMER не найдена в БД. Запустите скрипт инициализации или добавьте роль вручную.");
                });
        
        log.debug("Роль найдена: {}", userRole.getName());
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        // 7. Сохранение
        log.debug("Сохранение пользователя в БД...");
        try {
            User savedUser = userRepository.save(user);
            log.info("=== УСПЕХ === Пользователь успешно зарегистрирован: {} (ID={})", savedUser.getEmail(), savedUser.getId());
            return convertToDto(savedUser);
        } catch (Exception e) {
            log.error("=== ОШИБКА ПРИ СОХРАНЕНИИ === Не удалось сохранить пользователя в БД", e);
            throw e; // Пробрасываем исключение дальше, чтобы транзакция откатилась
        }
    }

    public List<UserDto> getAllUsers() {
        log.debug("Запрос списка всех пользователей");
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        log.debug("Найдено пользователей: {}", users.size());
        return users;
    }

    // Приватный метод маппинга
    private UserDto convertToDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name()) 
                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .bonusBalance(user.getBonusBalance())
                .roles(roleNames)
                .build();
    }

    // --- Методы для Spring Security ---

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Загрузка деталей пользователя для аутентификации: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Аутентификация не удалась: пользователь '{}' не найден", username);
                    return new UsernameNotFoundException("Пользователь не найден: " + username);
                });

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());

        log.debug("Пользователь {} загружен с ролями: {}", username, authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                authorities
        );
    }
}