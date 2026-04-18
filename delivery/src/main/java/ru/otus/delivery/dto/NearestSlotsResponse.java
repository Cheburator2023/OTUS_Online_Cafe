package ru.otus.delivery.dto;

import java.util.List;

public record NearestSlotsResponse(
        String message,
        List<DeliverySlotResponse> nearestSlots
) {}