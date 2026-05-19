package com.example.bakery.config;

import com.example.bakery.feature.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final CustomLoginSuccessHandler loginSuccessHandler; // Внедряем обработчик

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()
                
                .requestMatchers("/", "/index", "/home").permitAll()
                .requestMatchers("/auth/login", "/auth/register", "/auth/api/**").permitAll()
                .requestMatchers("/products", "/products/**").permitAll() 
                .requestMatchers("/categories", "/categories/**").permitAll()
                .requestMatchers("/api/products/**", "/api/categories/**", "/api/reviews/**").permitAll()
                .requestMatchers("/cart/**", "/api/cart/**").permitAll()

                .requestMatchers("/orders/admin/**", "/admin/**").hasRole("ADMIN")

                .requestMatchers("/profile", "/profile/**").authenticated()
                .requestMatchers("/orders/my", "/orders/create", "/orders/checkout").authenticated()
                .requestMatchers("/orders/**").permitAll()
                
                .anyRequest().authenticated()
            )
            
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login") // Явно указываем URL обработки
                .successHandler(loginSuccessHandler) // ПОДКЛЮЧАЕМ НАШ ОБРАБОТЧИК
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