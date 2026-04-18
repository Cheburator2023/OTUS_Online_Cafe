package ru.otus.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.delivery.dto.DeliveryResponse;
import ru.otus.delivery.dto.DeliverySlotResponse;
import ru.otus.delivery.dto.ReleaseDeliveryRequest;
import ru.otus.delivery.dto.ReserveDeliveryRequest;
import ru.otus.delivery.exception.DeliverySlotNotAvailableException;
import ru.otus.delivery.exception.DeliverySlotNotAvailableWithSlotsException;
import ru.otus.delivery.exception.DeliverySlotNotFoundException;
import ru.otus.delivery.model.DeliverySlot;
import ru.otus.delivery.repository.DeliverySlotRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliverySlotRepository slotRepository;

    @Transactional(readOnly = true)
    public List<DeliverySlotResponse> findNearestAvailableSlots(LocalDateTime afterTime, int limit) {
        // Ищем все слоты после указанного времени, которые не зарезервированы, сортируем по времени
        List<DeliverySlot> slots = slotRepository.findByTimeSlotGreaterThanEqualAndReservedFalseOrderByTimeSlotAsc(afterTime);
        return slots.stream()
                .limit(limit)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryResponse reserve(ReserveDeliveryRequest request) {
        LocalDateTime targetTime = request.deliveryTime();
        DeliverySlot slot;
        if (targetTime == null) {
            List<DeliverySlot> freeSlots = slotRepository.findByReservedFalseOrderByTimeSlotAsc();
            if (freeSlots.isEmpty()) {
                throw new DeliverySlotNotAvailableException("No free delivery slots available");
            }
            slot = freeSlots.get(0);
        } else {
            List<DeliverySlot> slots = slotRepository.findByTimeSlot(targetTime);
            if (slots.isEmpty()) {
                List<DeliverySlot> nearest = slotRepository.findByTimeSlotGreaterThanEqualAndReservedFalseOrderByTimeSlotAsc(targetTime);
                if (nearest.isEmpty()) {
                    throw new DeliverySlotNotAvailableException("No slots found near " + targetTime);
                }
                List<DeliverySlotResponse> nearestResponses = nearest.stream()
                        .limit(5)
                        .map(this::toResponse)
                        .collect(Collectors.toList());
                throw new DeliverySlotNotAvailableWithSlotsException(
                        "No slot at exact time. Nearest available slots: " +
                                nearest.stream().map(s -> s.getTimeSlot().toString()).collect(Collectors.joining(", ")),
                        nearestResponses);
            }
            slot = slots.stream()
                    .filter(s -> !s.isReserved())
                    .findFirst()
                    .orElseThrow(() -> new DeliverySlotNotAvailableException(
                            "All slots are already reserved for time: " + targetTime));
        }

        slot.setOrderId(request.orderId());
        slot.setReserved(true);
        slotRepository.save(slot);
        log.info("Reserved delivery slot {} for order {}", slot.getTimeSlot(), request.orderId());
        return new DeliveryResponse(true, "Delivery slot reserved");
    }

    @Transactional
    public void release(ReleaseDeliveryRequest request) {
        DeliverySlot slot = slotRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new DeliverySlotNotFoundException("No reservation found for order: " + request.orderId()));
        slot.setOrderId(null);
        slot.setReserved(false);
        slotRepository.save(slot);
        log.info("Released delivery slot for order {}", request.orderId());
    }

    private DeliverySlotResponse toResponse(DeliverySlot slot) {
        return new DeliverySlotResponse(slot.getId(), slot.getTimeSlot(), slot.getCourierId(), slot.isReserved());
    }
}