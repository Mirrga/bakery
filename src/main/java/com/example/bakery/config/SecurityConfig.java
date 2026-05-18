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
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
                // Публичные ресурсы
                .requestMatchers("/products/**", "/cart/**", "/api/products/**").permitAll()
                
                // Регистрация и логин доступны всем
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                
                // Админка только для ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Личный кабинет и заказы только для авторизованных
                .requestMatchers("/profile/**", "/orders/**").authenticated()
                
                // Все остальное требует входа
                .anyRequest().authenticated()
            )
            
            .formLogin(form -> form
                .loginPage("/auth/login") // Наша кастомная страница входа
                .defaultSuccessUrl("/products", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/products?logout=true")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}