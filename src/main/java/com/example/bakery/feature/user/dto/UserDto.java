package com.example.bakery.feature.user.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal bonusBalance;
    private Set<String> roles;
    private Boolean enabled;

    // Маппинг из Entity в DTO
    public static UserDto fromEntity(com.example.bakery.feature.user.entity.User user) {
        if (user == null) {
            return null;
        }

        // Безопасная обработка ролей: проверка на null самой коллекции и элементов
        Set<String> roleNames = user.getRoles() != null 
            ? user.getRoles().stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> role.getName().name())
                .collect(Collectors.toSet())
            : Set.of(); // Возвращаем пустой набор, если ролей нет

        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .bonusBalance(user.getBonusBalance())
                .enabled(user.getEnabled())
                .roles(roleNames)
                .build();
    }
}