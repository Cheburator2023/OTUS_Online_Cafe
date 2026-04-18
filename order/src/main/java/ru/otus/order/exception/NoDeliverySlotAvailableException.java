package ru.otus.order.exception;

import ru.otus.order.dto.DeliverySlotResponse;
import java.util.List;

public class NoDeliverySlotAvailableException extends RuntimeException {
    private final List<DeliverySlotResponse> nearestSlots;

    public NoDeliverySlotAvailableException(String message, List<DeliverySlotResponse> nearestSlots) {
        super(message);
        this.nearestSlots = nearestSlots;
    }

    public List<DeliverySlotResponse> getNearestSlots() {
        return nearestSlots;
    }
}