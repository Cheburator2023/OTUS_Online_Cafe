package ru.otus.order.exception;

import ru.otus.order.dto.DeliverySlotResponse;

import java.util.List;

public class StockNotAvailableException extends RuntimeException {
    private final int preparationTimeMinutes;
    private final List<DeliverySlotResponse> nearestSlots;

    public StockNotAvailableException(String message, int preparationTimeMinutes, List<DeliverySlotResponse> nearestSlots) {
        super(message);
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.nearestSlots = nearestSlots;
    }

    public int getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    public List<DeliverySlotResponse> getNearestSlots() {
        return nearestSlots;
    }
}