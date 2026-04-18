package ru.otus.order.dto;

import java.util.List;

public record NearestSlotsResponse(
        String message,
        List<DeliverySlotResponse> nearestSlots
) {}