package ru.otus.order.dto;

public record CartItemResponse(
        Long id,
        Long productId,
        Long quantity
) {}