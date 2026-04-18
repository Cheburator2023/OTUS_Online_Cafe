package ru.otus.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.otus.order.dto.*;
import ru.otus.order.service.CartService;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Tag(name = "Cart API")
public class CartController {

    private final CartService cartService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create empty cart for user")
    public void createCart(@RequestParam Long userId) {
        cartService.createCart(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get cart by user ID")
    public CartResponse getCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to cart")
    public CartResponse addItem(@PathVariable Long userId, @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(userId, request);
    }

    @PutMapping("/{userId}/items")
    @Operation(summary = "Replace cart items with new list")
    public CartResponse replaceItems(@PathVariable Long userId, @Valid @RequestBody ReplaceCartItemsRequest request) {
        return cartService.replaceItems(userId, request.items());
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public CartResponse removeItem(@PathVariable Long userId, @PathVariable Long itemId) {
        return cartService.removeItem(userId, itemId);
    }

    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Create order from cart")
    public OrderResponse checkout(@PathVariable Long userId, @Valid @RequestBody ConfirmOrderRequest request) {
        return cartService.checkout(userId, request);
    }
}