package ru.otus.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplaceCartItemsRequest(
        @NotNull @Valid List<CartItemRequest> items
) {}