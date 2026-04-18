package ru.otus.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.otus.order.dto.CreateOrderRequest;
import ru.otus.order.dto.OrderResponse;
import ru.otus.order.service.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an order (idempotent)")
    public OrderResponse createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(idempotencyKey, request);
    }

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "Confirm pending order (accept waiting)")
    public OrderResponse confirmOrder(@PathVariable Long orderId, @RequestParam boolean accept) {
        return orderService.confirmPendingOrder(orderId, accept);
    }

    @PostMapping("/{orderId}/deliver")
    @Operation(summary = "Mark order as delivered (by courier)")
    public void deliverOrder(@PathVariable Long orderId) {
        orderService.deliverOrder(orderId);
    }

    @PostMapping("/{orderId}/confirm-receipt")
    @Operation(summary = "Confirm order receipt by customer")
    public void confirmReceipt(@PathVariable Long orderId) {
        orderService.confirmReceipt(orderId);
    }
}