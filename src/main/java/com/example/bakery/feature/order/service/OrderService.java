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
import lombok.RequiredArgsConstructor;
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

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public Order createOrderFromCart(String sessionId, String username) {
        Cart cart = getValidCart(sessionId);
        User user = getValidUser(username);

        Order order = createNewOrder(user);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product == null) throw new RuntimeException("Товар в корзине не найден");

            OrderItem orderItem = createOrderItem(order, product, cartItem.getQuantity());
            order.addItem(orderItem);
            
            // Теперь типы совпадают (BigDecimal * BigDecimal)
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(totalAmount); // Прямое присваивание
        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);
        return savedOrder;
    }

    public OrderDto createOrderFromDto(OrderRequestDto requestDto, String username) {
        User user = getValidUser(username);
        Order order = createNewOrder(user);
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequestDto.OrderItemRequest itemRequest : requestDto.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Товар не найден: " + itemRequest.getProductId()));

            OrderItem orderItem = createOrderItem(order, product, itemRequest.getQuantity());
            order.addItem(orderItem);

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        return mapToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderDtoById(Long orderId) {
        return mapToDto(getOrderById(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrdersDto(String username) {
        User user = getValidUser(username);
        List<Order> orders = orderRepository.findAllByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrdersDto() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Order createNewOrder(User user) {
        return Order.builder().user(user).status(OrderStatus.NEW).items(new ArrayList<>()).build();
    }

    private OrderItem createOrderItem(Order order, Product product, int quantity) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice()) // Прямая передача BigDecimal
                .build();
    }

    private Cart getValidCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Корзина пуста или не найдена"));
        return cart;
    }

    private User getValidUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private OrderDto mapToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        if (order.getUser() != null) dto.setUserId(order.getUser().getId());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount()); // Прямая передача
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
            itemDto.setPrice(item.getPrice()); // Прямая передача
            
            // Расчет subtotal прямо в DTO
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