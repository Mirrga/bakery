package com.example.bakery.feature.order.service;

import com.example.bakery.feature.cart.entity.Cart;
import com.example.bakery.feature.cart.entity.CartItem;
import com.example.bakery.feature.cart.repository.CartRepository;
import com.example.bakery.feature.order.dto.OrderDto;
import com.example.bakery.feature.order.dto.OrderItemDto;
import com.example.bakery.feature.order.dto.OrderRequestDto;
import com.example.bakery.feature.order.entity.Order;
import com.example.bakery.feature.order.entity.OrderItem;
import com.example.bakery.feature.order.entity.OrderStatus;
import com.example.bakery.feature.order.repository.OrderRepository;
import com.example.bakery.feature.product.entity.Product;
import com.example.bakery.feature.product.repository.ProductRepository;
import com.example.bakery.feature.user.entity.User;
import com.example.bakery.feature.user.repository.UserRepository;
import com.example.bakery.utils.ExternalApiService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ExternalApiService externalApiService; // Внедрен сервис внешнего API

    public Order createOrderFromCart(String sessionId, String username) {
        log.info("Начало оформления заказа из корзины. Пользователь: {}, Сессия: {}", username, sessionId);
        
        Cart cart = getValidCart(sessionId);
        User user = getValidUser(username);

        Order order = createNewOrder(user);
        BigDecimal totalAmount = BigDecimal.ZERO;
        int itemCount = 0;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product == null) {
                log.error("Товар в корзине не найден для элемента ID: {}", cartItem.getId());
                throw new RuntimeException("Товар в корзине не найден");
            }

            OrderItem orderItem = createOrderItem(order, product, cartItem.getQuantity());
            order.addItem(orderItem);
            
            BigDecimal itemSum = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemSum);
            itemCount += cartItem.getQuantity();
            
            log.debug("Добавлен товар в заказ: {} (кол-во: {}, цена: {})", product.getName(), cartItem.getQuantity(), itemSum);
        }

        order.setTotalAmount(totalAmount);
        log.info("Создан заказ. Всего товаров: {}, Сумма: {}", itemCount, totalAmount);
        
        // ================= ИНТЕГРАЦИЯ С ВНЕШНИМ API =================
        // Получаем курс валют перед сохранением заказа для логирования или бизнес-логики
        double usdRate = externalApiService.getUsdToRubRate();

        if (usdRate > 0) {
            // Рассчитываем сумму в USD (курс API показывает, сколько USD за 1 RUB, поэтому умножаем)
            // Если API возвращает курс относительно базовой валюты (RUB), то значение для USD будет маленьким (например, 0.011)
            // Чтобы получить сумму в USD: TotalRUB * Rate(USD per RUB)
            BigDecimal totalInUsd = totalAmount.multiply(BigDecimal.valueOf(usdRate));
            
            log.info("💰 Заказ №{} создан. Сумма: {} RUB. Курс USD/RUB (API): {}. Эквивалент в USD: {}", 
                     order.getId(), totalAmount, usdRate, totalInUsd);
        } else {
            log.warn("⚠️ Заказ №{} создан. Сумма: {} RUB. Не удалось получить актуальный курс валют из внешнего API.", 
                     order.getId(), totalAmount);
        }
        // ============================================================
        

        Order savedOrder = orderRepository.save(order);
        log.info("Заказ №{} успешно сохранен в БД", savedOrder.getId());

        cart.getItems().clear();
        cartRepository.save(cart);
        log.debug("Корзина пользователя {} очищена после оформления заказа", username);
        
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public double getCurrentUsdRate() {
        return externalApiService.getUsdToRubRate();
    }

    public OrderDto createOrderFromDto(OrderRequestDto requestDto, String username) {
        log.info("Создание заказа из DTO. Пользователь: {}", username);
        
        User user = getValidUser(username);
        Order order = createNewOrder(user);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequestDto.OrderItemRequest itemRequest : requestDto.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> {
                        log.warn("Товар не найден при создании заказа из DTO. ID товара: {}", itemRequest.getProductId());
                        return new RuntimeException("Товар не найден: " + itemRequest.getProductId());
                    });

            OrderItem orderItem = createOrderItem(order, product, itemRequest.getQuantity());
            order.addItem(orderItem);

            BigDecimal itemSum = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemSum);
        }

        order.setTotalAmount(totalAmount);
        
        // Опционально: можно добавить логику с курсом валют и здесь, если нужно
        double usdRate = externalApiService.getUsdToRubRate();
        if (usdRate > 0) {
             log.info("💰 Заказ (DTO) создан. Сумма: {} RUB. Курс USD: {}", totalAmount, usdRate);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Заказ №{} успешно создан из DTO. Сумма: {}", savedOrder.getId(), totalAmount);
        
        return mapToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderDtoById(Long orderId) {
        log.debug("Запрос данных заказа (DTO) по ID: {}", orderId);
        return mapToDto(getOrderById(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrdersDto(String username) {
        log.debug("Запрос списка заказов (DTO) для пользователя: {}", username);
        User user = getValidUser(username);
        List<Order> orders = orderRepository.findAllByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));
        log.debug("Найдено заказов для пользователя {}: {}", username, orders.size());
        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrdersDto() {
        log.debug("Запрос списка всех заказов (DTO)");
        List<OrderDto> result = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::mapToDto).collect(Collectors.toList());
        log.debug("Всего заказов в системе: {}", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Запрос списка всех заказов (Entity)");
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Order createNewOrder(User user) {
        Order order = Order.builder().user(user).status(OrderStatus.NEW).items(new ArrayList<>()).build();
        log.debug("Создан новый объект заказа для пользователя ID: {}", user.getId());
        return order;
    }

    private OrderItem createOrderItem(Order order, Product product, int quantity) {
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();
        log.trace("Создан элемент заказа: Товар={}, Кол-во={}, Цена={}", product.getName(), quantity, product.getPrice());
        return item;
    }

    private Cart getValidCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.warn("Корзина пуста или не найдена для сессии: {}", sessionId);
                    return new RuntimeException("Корзина пуста или не найдена");
                });
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Корзина найдена, но пуста для сессии: {}", sessionId);
            throw new RuntimeException("Корзина пуста");
        }
        
        log.debug("Корзина найдена. Количество товаров: {}", cart.getItems().size());
        return cart;
    }

    private User getValidUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь не найден: {}", username);
                    return new RuntimeException("Пользователь не найден");
                });
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        log.debug("Поиск заказа по ID: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Заказ не найден с ID: {}", orderId);
                    return new RuntimeException("Заказ не найден");
                });
    }

    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        log.info("Обновление статуса заказа №{}. Новый статус: {}", orderId, newStatus);
        Order order = getOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();
        
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        
        log.info("Статус заказа №{} изменен с {} на {}", orderId, oldStatus, newStatus);
        return updated;
    }

    private OrderDto mapToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        if (order.getUser() != null) dto.setUserId(order.getUser().getId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItemDto> itemDtos = order.getItems().stream().map(item -> {
            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setId(item.getId());
            if (item.getProduct() != null) {
                itemDto.setProductId(item.getProduct().getId());
                itemDto.setProductName(item.getProduct().getName());
                itemDto.setProductImageUrl(item.getProduct().getImageUrl());
            }
            itemDto.setQuantity(item.getQuantity());
            itemDto.setPrice(item.getPrice());
            
            if (item.getPrice() != null && item.getQuantity() != null) {
                itemDto.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            } else {
                itemDto.setSubtotal(BigDecimal.ZERO);
            }
            return itemDto;
        }).collect(Collectors.toList());
        
        dto.setItems(itemDtos);
        return dto;
    }
}