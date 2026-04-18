package ru.otus.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        Long quantity,
        BigDecimal price
) {}