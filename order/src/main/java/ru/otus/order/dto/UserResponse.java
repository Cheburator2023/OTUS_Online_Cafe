package ru.otus.order.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String deliveryAddress,
        Instant createdAt
) {}