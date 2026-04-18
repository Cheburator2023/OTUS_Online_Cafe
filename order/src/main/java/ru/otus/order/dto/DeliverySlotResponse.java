package ru.otus.order.dto;

import java.time.LocalDateTime;

public record DeliverySlotResponse(
        Long slotId,
        LocalDateTime timeSlot,
        Long courierId
) {}