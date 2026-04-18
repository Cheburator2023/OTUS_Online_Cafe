package ru.otus.user.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String deliveryAddress,
        Instant createdAt
) {}