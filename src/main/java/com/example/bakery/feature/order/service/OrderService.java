package com.example.bakery.feature.order.service;

import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.entity.CartItem;
import com.example.bakery.feature.cart.repository.CartRepository;
import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.order.entity.OrderItem;
import com.example.bakery.feature.order.entity.OrderStatus;
import com.example.bakery.feature.order.repository.OrderRepository;
import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    /**
     * Оформить заказ из корзины
     * @param sessionId ID сессии корзины
     * @param username имя пользователя (для привязки заказа)
     * @return созданный заказ
     */
    public Order createOrderFromCart(String sessionId, String username) {
        // Получаем корзину
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));

        // Проверяем, что в корзине есть товары
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Корзина пуста");
        }

        // Получаем пользователя
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));

        // Создаем заказ
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.NEW)
                .items(new java.util.ArrayList<>())
                .build();

        // Переносим товары из корзины в заказ
        double totalAmount = 0.0;
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getProduct().getPrice().doubleValue())
                    .build();

            order.addItem(orderItem);
            totalAmount += cartItem.getProduct().getPrice().doubleValue() * cartItem.getQuantity();
        }

        order.setTotalAmount(totalAmount);

        // Сохраняем заказ
        Order savedOrder = orderRepository.save(order);

        // Очищаем корзину после успешного оформления заказа
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    /**
     * Получить все заказы пользователя
     */
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
        return orderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .toList();
    }

    /**
     * Получить все заказы (для администратора)
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Получить заказ по ID
     */
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + orderId));
    }

    /**
     * Обновить статус заказа
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
}