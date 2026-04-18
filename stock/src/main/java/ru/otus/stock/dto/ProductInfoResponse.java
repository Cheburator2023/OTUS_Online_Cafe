package ru.otus.stock.dto;

import java.math.BigDecimal;

public record ProductInfoResponse(
        Long productId,
        Long requestedQuantity,
        boolean available,
        int preparationTimeMinutes,
        BigDecimal price
) {}