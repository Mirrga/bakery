package com.example.bakery.config;

import com.example.bakery.feature.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ВРЕМЕННО ОТКЛЮЧАЕМ CSRF, чтобы исключить его влияние на доступ к странице регистрации
            .csrf(csrf -> csrf.disable()) 
            
            .authorizeHttpRequests(auth -> auth
                // 1. Статические ресурсы - должны быть ПЕРВЫМИ
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                
                // 2. Страницы аутентификации - ДО anyRequest().authenticated()
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/auth/api/**").permitAll()
                
                // 3. Публичные страницы
                .requestMatchers("/", "/index", "/products", "/products/**").permitAll() 
                
                // 4. Админка
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // 5. Личный кабинет
                .requestMatchers("/profile/**", "/orders/**").authenticated()
                
                // 6. ВСЁ остальное требует входа. 
                // Внимание: если вы добавите сюда новый путь, он будет закрыт.
                .anyRequest().authenticated()
            )
            
            .formLogin(form -> form
                .loginPage("/auth/login")
                .defaultSuccessUrl("/products", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/products?logout=true")
                .permitAll()
            )
            // Важно: разрешаем фреймы (если используете h2 console для тестов, иначе можно убрать)
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); 

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}