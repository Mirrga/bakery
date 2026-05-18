package com.example.bakery.config;

import com.example.bakery.feature.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // 1. Импорт аннотации
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // 2. Включаем поддержку @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ВРЕМЕННО ОТКЛЮЧАЕМ CSRF для форм (так как используем свой FormToken механизм)
            .csrf(csrf -> csrf.disable()) 
            
            .authorizeHttpRequests(auth -> auth
                // 1. Статические ресурсы - должны быть ПЕРВЫМИ
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                
                // 2. Страницы аутентификации и публичные API
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/auth/api/**").permitAll()
                
                // 3. Публичные страницы и API продуктов/категорий
                .requestMatchers("/", "/index").permitAll()
                .requestMatchers("/products", "/products/**").permitAll() 
                .requestMatchers("/categories", "/categories/**").permitAll()
                .requestMatchers("/api/products/**", "/api/categories/**").permitAll()
                
                // 4. Публичный доступ к отзывам (чтение), но создание требует авторизации (контролируется в контроллере/сервисе)
                .requestMatchers("/api/reviews/**").permitAll() 

                // 5. Корзина: доступна всем (гостям и пользователям) по sessionId
                .requestMatchers("/cart/**", "/api/cart/**").permitAll()

                // 6. Админка: Доступ ТОЛЬКО администраторам
                .requestMatchers("/orders/admin/**", "/admin/**").hasRole("ADMIN")
                
                // 7. Личный кабинет и заказы: Требуется авторизация
                .requestMatchers("/profile/**", "/orders/my", "/orders/create", "/users/api/me").authenticated()
                
                // 8. ВСЁ остальное требует входа
                .anyRequest().authenticated()
            )
            
            .formLogin(form -> form
                .loginPage("/auth/login")
                .defaultSuccessUrl("/products", true) // Редирект после успешного входа
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/products?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); // Для H2 console, если используется

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}