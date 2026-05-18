package com.example.bakery.feature.user.dto;

import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.UserRole; // Проверь, нужен ли этот импорт, обычно достаточно Role
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class UserDto {
    private Long id;

    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
    private String firstName;

    @NotBlank(message = "Фамилия обязательна")
    @Size(min = 2, max = 50, message = "Фамилия должна быть от 2 до 50 символов")
    private String lastName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен быть минимум 6 символов")
    private String password;

    // Поле для подтверждения пароля (не сохраняется в БД, только для формы)
    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword; 

    private BigDecimal bonusBalance;
    private Set<String> roles; // Меняем на Set<String> для простоты передачи имен ролей

    // Маппинг из Entity в DTO
    public static UserDto fromEntity(com.example.bakery.feature.user.entity.User user) {
        // Явно собираем имена ролей в Set<String>
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name()) // Если getName() возвращает Enum, добавляем .name()
                // ИЛИ, если getName() уже возвращает String:
                // .map(role -> role.getName()) 
                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .email(user.getEmail())
                .bonusBalance(user.getBonusBalance())
                .roles(roleNames) // Передаем готовый Set<String>
                .build();
    }
}