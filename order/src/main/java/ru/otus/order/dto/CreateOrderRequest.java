package ru.otus.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotNull @Email String email,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        List<OrderItemRequest> items,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime deliveryTime,
        boolean acceptWaiting
) {}