package com.example.bakery.config;

import com.example.bakery.feature.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
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
                // 1. Статические ресурсы (СТРОГО ПЕРВЫМИ)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()
                
                // 2. Публичные страницы и API
                .requestMatchers("/", "/index", "/home").permitAll()
                .requestMatchers("/auth/login", "/auth/register", "/auth/api/**").permitAll()
                .requestMatchers("/products", "/products/**").permitAll() 
                .requestMatchers("/categories", "/categories/**").permitAll()
                .requestMatchers("/api/products/**", "/api/categories/**", "/api/reviews/**").permitAll()
                .requestMatchers("/cart/**", "/api/cart/**").permitAll()

                // 3. Админка
                .requestMatchers("/orders/admin/**", "/admin/**").hasRole("ADMIN")

                // 4. ЗАЩИЩЕННЫЕ МАРШРУТЫ (Самые важные ставим ВЫШЕ общих)
                // ВАЖНО: Правило для профиля ставим ДО любых правил для /orders
                .requestMatchers("/profile", "/profile/**").authenticated()
                
                // Правила для заказов
                .requestMatchers("/orders/my", "/orders/create", "/orders/checkout").authenticated()
                .requestMatchers("/orders/**").permitAll() // Остальные заказы (например, просмотр) публичны
                
                // 5. ВСЁ остальное требует входа
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
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); 

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}