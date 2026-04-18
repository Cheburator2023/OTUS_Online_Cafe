package ru.otus.order.dto;

import ru.otus.order.model.Order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        BigDecimal amount,
        OrderStatus status,
        Instant createdAt,
        LocalDateTime deliveryTime,
        Integer preparationTimeMinutes,
        List<OrderItemResponse> items
) {}