package ru.otus.delivery.dto;

import java.time.LocalDateTime;

public record DeliverySlotResponse(
        Long id,
        LocalDateTime timeSlot,
        Long courierId,
        boolean reserved
) {}