package ru.otus.delivery.exception;

import ru.otus.delivery.dto.DeliverySlotResponse;
import java.util.List;

public class DeliverySlotNotAvailableWithSlotsException extends RuntimeException {
    private final List<DeliverySlotResponse> nearestSlots;

    public DeliverySlotNotAvailableWithSlotsException(String message, List<DeliverySlotResponse> nearestSlots) {
        super(message);
        this.nearestSlots = nearestSlots;
    }

    public List<DeliverySlotResponse> getNearestSlots() {
        return nearestSlots;
    }
}