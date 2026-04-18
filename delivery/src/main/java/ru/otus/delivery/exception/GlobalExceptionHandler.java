package ru.otus.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.otus.delivery.dto.ErrorResponse;
import ru.otus.delivery.dto.NearestSlotsResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeliverySlotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DeliverySlotNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DELIVERY_SLOT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DeliverySlotNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNotAvailable(DeliverySlotNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DELIVERY_SLOT_NOT_AVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(DeliverySlotNotAvailableWithSlotsException.class)
    public ResponseEntity<NearestSlotsResponse> handleNotAvailableWithSlots(DeliverySlotNotAvailableWithSlotsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new NearestSlotsResponse(ex.getMessage(), ex.getNearestSlots()));
    }
}