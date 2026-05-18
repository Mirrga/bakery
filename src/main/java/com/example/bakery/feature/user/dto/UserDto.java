package com.example.bakery.feature.user.dto;

import com.example.bakery.feature.user.entity.Role;
import com.example.bakery.feature.user.entity.UserRole;
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
    private String username;
    private String email;
    private BigDecimal bonusBalance;
    private Set<UserRole> roles;

    public static UserDto fromEntity(com.example.bakery.feature.user.entity.User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .bonusBalance(user.getBonusBalance())
                // Явный маппинг, чтобы избежать проблем с методом ссылки ::
                .roles(user.getRoles().stream()
                        .map(Role::getName) 
                        .collect(Collectors.toSet()))
                .build();
    }
    
    // Альтернативный метод создания, если билдер вызывает проблемы в конкретном месте вызова
    public static UserDto create(Long id, String username, String email, BigDecimal bonusBalance, Set<UserRole> roles) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setBonusBalance(bonusBalance);
        dto.setRoles(roles);
        return dto;
    }
}