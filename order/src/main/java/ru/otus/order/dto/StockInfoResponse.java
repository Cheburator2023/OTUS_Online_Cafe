package ru.otus.order.dto;

import java.math.BigDecimal;

public record StockInfoResponse(
        Long productId,
        Long requestedQuantity,
        boolean available,
        int preparationTimeMinutes,
        BigDecimal price
) {}