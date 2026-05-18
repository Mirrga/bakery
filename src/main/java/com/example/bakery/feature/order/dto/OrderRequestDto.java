package com.example.bakery.feature.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequestDto {
    
    @NotEmpty(message = "Заказ должен содержать хотя бы один товар")
    private List<OrderItemRequest> items;

    @NotBlank(message = "Адрес доставки обязателен")
    private String shippingAddress;

    @NotBlank(message = "Телефон обязателен")
    private String phone;

    @NotBlank(message = "Email обязателен")
    @jakarta.validation.constraints.Email(message = "Некорректный формат email")
    private String email;

    // Если нужно сохранять способ оплаты отдельно, иначе можно убрать
    private String paymentMethod; 

    // Вложенный класс для элемента заказа при создании
    @Data
    public static class OrderItemRequest {
        @NotNull(message = "ID продукта обязателен")
        private Long productId;
        
        @NotNull(message = "Количество обязательно")
        @jakarta.validation.constraints.Min(value = 1, message = "Количество должно быть больше 0")
        private Integer quantity;
    }
}