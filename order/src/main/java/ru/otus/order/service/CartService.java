package ru.otus.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.order.dto.*;
import ru.otus.order.exception.CartNotFoundException;
import ru.otus.order.model.Cart;
import ru.otus.order.model.CartItem;
import ru.otus.order.repository.CartItemRepository;
import ru.otus.order.repository.CartRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Lazy
    private final OrderService orderService;

    @Transactional
    public void createCart(Long userId) {
        if (cartRepository.findByUserId(userId).isEmpty()) {
            Cart cart = new Cart(userId);
            cartRepository.save(cart);
            log.info("Created empty cart for userId: {}", userId);
        }
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for userId: " + userId));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for userId: " + userId));

        // Проверяем, есть ли уже такой товар в корзине
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
            cartItemRepository.save(item);
            log.info("Increased quantity of product {} to {} in cart for userId {}",
                    request.productId(), item.getQuantity(), userId);
        } else {
            CartItem item = new CartItem(request.productId(), request.quantity());
            cart.addItem(item);
            cartItemRepository.save(item);
            log.info("Added product {} quantity {} to cart for userId {}",
                    request.productId(), request.quantity(), userId);
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for userId: " + userId));
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Item does not belong to this cart");
        }
        cart.removeItem(item);
        cartItemRepository.delete(item);
        log.info("Removed item {} from cart for userId {}", itemId, userId);
        return toResponse(cart);
    }

    @Transactional
    public OrderResponse checkout(Long userId, ConfirmOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for userId: " + userId));
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        var orderRequest = new CreateOrderRequest(
                userId,
                null,
                null,
                cart.getItems().stream()
                        .map(item -> new OrderItemRequest(item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList()),
                request.deliveryTime(),
                request.acceptWaiting()
        );
        return orderService.createOrderFromCart(orderRequest);
    }

    @Transactional
    public CartResponse replaceItems(Long userId, List<CartItemRequest> items) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for userId: " + userId));
        // Очищаем корзину
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        // Добавляем новые товары
        for (CartItemRequest request : items) {
            CartItem item = new CartItem(request.productId(), request.quantity());
            cart.addItem(item);
            cartItemRepository.save(item);
        }
        log.info("Replaced cart for userId {} with {} items", userId, items.size());
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for userId: " + userId));
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        log.info("Cleared all items from cart for userId {}", userId);
    }

    private CartResponse toResponse(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getItems().stream()
                        .map(item -> new CartItemResponse(item.getId(), item.getProductId(), item.getQuantity()))
                        .collect(Collectors.toList())
        );
    }
}