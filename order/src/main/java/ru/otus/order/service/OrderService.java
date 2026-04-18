package ru.otus.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.order.dto.*;
import ru.otus.order.exception.NoDeliverySlotAvailableException;
import ru.otus.order.model.IdempotencyRecord;
import ru.otus.order.model.Order;
import ru.otus.order.model.Order.OrderStatus;
import ru.otus.order.model.OrderItem;
import ru.otus.order.repository.IdempotencyRepository;
import ru.otus.order.repository.OrderItemRepository;
import ru.otus.order.repository.OrderRepository;
import ru.otus.order.service.client.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final BillingServiceClient billingClient;
    private final NotificationServiceClient notificationClient;
    private final StockServiceClient stockClient;
    private final DeliveryServiceClient deliveryClient;
    private final UserServiceClient userClient;

    @Lazy
    @Autowired
    private CartService cartService;

    @Value("${delivery.min-delivery-time-minutes:30}")
    private int minDeliveryTimeMinutes;

    @Transactional
    public OrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        log.info("Creating order with idempotencyKey: {}", idempotencyKey);
        var existingRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            Order existingOrder = existingRecord.get().getOrder();
            log.info("Idempotency key {} already processed, returning existing order {}", idempotencyKey, existingOrder.getId());
            return toResponse(existingOrder);
        }
        OrderResponse orderResponse = createOrderFromCart(request);
        Order order = orderRepository.findById(orderResponse.id())
                .orElseThrow(() -> new RuntimeException("Order not found after creation"));
        saveIdempotencyRecord(idempotencyKey, order);
        return orderResponse;
    }

    private void saveIdempotencyRecord(String idempotencyKey, Order order) {
        try {
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, order);
            idempotencyRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to save idempotency record for key {}", idempotencyKey, e);
        }
    }

    @Transactional
    public OrderResponse createOrderFromCart(CreateOrderRequest request) {
        log.info("Creating order from cart for userId: {}", request.userId());
        // Определяем время доставки: если не указано, берём ближайший свободный слот
        LocalDateTime finalDeliveryTime = request.deliveryTime();
        if (finalDeliveryTime == null) {
            List<DeliverySlotResponse> nearestSlots = deliveryClient.findNearestSlots(LocalDateTime.now(), 1);
            if (nearestSlots.isEmpty()) {
                throw new RuntimeException("No delivery slots available");
            }
            finalDeliveryTime = nearestSlots.get(0).timeSlot();
            log.info("No delivery time specified, using nearest available slot: {}", finalDeliveryTime);
        }

        List<StockInfoResponse> allItemsInfo = request.items().stream()
                .map(item -> stockClient.getProductInfo(item.productId(), item.quantity()))
                .collect(Collectors.toList());

        List<StockInfoResponse> availableItems = allItemsInfo.stream()
                .filter(StockInfoResponse::available)
                .collect(Collectors.toList());
        List<StockInfoResponse> waitingItems = allItemsInfo.stream()
                .filter(info -> !info.available() && info.preparationTimeMinutes() > 0)
                .collect(Collectors.toList());

        // ПРОВЕРКА: если клиент не согласен ждать, но есть товары на готовку -> ошибка (заказ НЕ создаётся)
        if (!request.acceptWaiting() && !waitingItems.isEmpty()) {
            String waitingProducts = waitingItems.stream()
                    .map(info -> "Product " + info.productId() + " (" + info.preparationTimeMinutes() + " min)")
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("The following products require preparation: " + waitingProducts +
                    ". Please accept waiting or remove them from cart.");
        }

        List<StockInfoResponse> itemsToOrder = request.acceptWaiting() ? allItemsInfo : availableItems;
        if (itemsToOrder.isEmpty()) {
            throw new RuntimeException("No items available for order");
        }

        BigDecimal totalAmount = itemsToOrder.stream()
                .map(info -> info.price().multiply(BigDecimal.valueOf(info.requestedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasWaitingItems = itemsToOrder.stream().anyMatch(info -> !info.available());
        OrderStatus initialStatus = hasWaitingItems ? OrderStatus.PENDING : OrderStatus.CONFIRMED;

        Order order = new Order(request.userId(), totalAmount, initialStatus, finalDeliveryTime);
        if (hasWaitingItems) {
            int maxPreparationTime = itemsToOrder.stream()
                    .filter(info -> !info.available())
                    .mapToInt(StockInfoResponse::preparationTimeMinutes)
                    .max().orElse(0);
            order.setPreparationTimeMinutes(maxPreparationTime);
        }
        order = orderRepository.save(order);

        for (StockInfoResponse info : itemsToOrder) {
            OrderItem item = new OrderItem(info.productId(), info.requestedQuantity(), info.price());
            order.addItem(item);
        }
        orderItemRepository.saveAll(order.getItems());

        if (initialStatus == OrderStatus.CONFIRMED) {
            try {
                boolean withdrawSuccess = billingClient.withdraw(request.userId(), totalAmount);
                if (!withdrawSuccess) {
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                    sendNotification(request.userId(), request.email(), "Insufficient funds");
                    return toResponse(order);
                }

                for (StockInfoResponse info : itemsToOrder) {
                    stockClient.reserve(info.productId(), info.requestedQuantity(), order.getId());
                }

                deliveryClient.reserve(order.getId(), finalDeliveryTime);

                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                sendNotification(request.userId(), null, "Order confirmed");
                cartService.clearCart(request.userId());
            } catch (Exception e) {
                compensate(order, request, totalAmount);
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                sendNotification(request.userId(), null, "Order failed: " + e.getMessage());
            }
        } else {
            String waitingProducts = waitingItems.stream()
                    .map(info -> "Product " + info.productId() + " (" + info.preparationTimeMinutes() + " min)")
                    .collect(Collectors.joining(", "));
            String message = String.format(
                    "Your order requires confirmation. The following products need preparation: %s. Total waiting time: %d minutes. Please confirm or cancel.",
                    waitingProducts, order.getPreparationTimeMinutes()
            );
            sendNotification(request.userId(), null, message);
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse confirmPendingOrder(Long orderId, boolean accept) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending state");
        }
        if (!accept) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            sendNotification(order.getUserId(), null, "Order cancelled by user");
            return toResponse(order);
        }

        try {
            LocalDateTime earliestDeliveryTime = LocalDateTime.now();
            if (order.getPreparationTimeMinutes() != null && order.getPreparationTimeMinutes() > 0) {
                earliestDeliveryTime = earliestDeliveryTime.plusMinutes(order.getPreparationTimeMinutes())
                        .plusMinutes(minDeliveryTimeMinutes);
            }
            List<DeliverySlotResponse> nearestSlots = deliveryClient.findNearestSlots(earliestDeliveryTime, 5);
            if (nearestSlots.isEmpty()) {
                throw new NoDeliverySlotAvailableException("No delivery slots available after " + earliestDeliveryTime, nearestSlots);
            }
            DeliverySlotResponse selectedSlot = nearestSlots.get(0);
            order.setDeliveryTime(selectedSlot.timeSlot());

            boolean withdrawSuccess = billingClient.withdraw(order.getUserId(), order.getAmount());
            if (!withdrawSuccess) {
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                sendNotification(order.getUserId(), null, "Insufficient funds");
                return toResponse(order);
            }

            for (OrderItem item : order.getItems()) {
                stockClient.reserve(item.getProductId(), item.getQuantity(), order.getId());
            }

            deliveryClient.reserve(order.getId(), order.getDeliveryTime());

            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(Instant.now());
            orderRepository.save(order);
            sendNotification(order.getUserId(), null, "Order confirmed");
            cartService.clearCart(order.getUserId());
        } catch (NoDeliverySlotAvailableException e) {
            throw e;
        } catch (Exception e) {
            compensate(order, null, order.getAmount());
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            sendNotification(order.getUserId(), null, "Order failed: " + e.getMessage());
        }
        return toResponse(order);
    }

    @Transactional
    public void deliverOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.READY_FOR_DELIVERY) {
            throw new RuntimeException("Order cannot be delivered, current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        orderRepository.save(order);
        deliveryClient.release(orderId);
        for (OrderItem item : order.getItems()) {
            stockClient.commitReservation(item.getProductId(), item.getQuantity());
        }
        sendNotification(order.getUserId(), null, "Your order has been delivered. Please confirm receipt.");
    }

    @Transactional
    public void confirmReceipt(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Order is not in delivered state");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        orderRepository.save(order);
        sendNotification(order.getUserId(), null, "Thank you for confirming order receipt!");
    }

    private void compensate(Order order, CreateOrderRequest request, BigDecimal amount) {
        try {
            billingClient.deposit(order.getUserId(), amount);
        } catch (Exception e) {
            log.error("Failed to deposit during compensation", e);
        }
        try {
            for (OrderItem item : order.getItems()) {
                stockClient.release(item.getProductId(), item.getQuantity(), order.getId());
            }
        } catch (Exception e) {
            log.error("Failed to release stock during compensation", e);
        }
        try {
            deliveryClient.release(order.getId());
        } catch (Exception e) {
            log.error("Failed to release delivery during compensation", e);
        }
    }

    private void sendNotification(Long userId, String email, String message) {
        String userEmail = email;
        if (userEmail == null) {
            userEmail = userClient.getUserEmail(userId);
        }
        if (userEmail == null) {
            log.warn("Cannot send notification: no email for user {}", userId);
            return;
        }
        notificationClient.sendNotification(new NotificationRequest(userId, userEmail, message));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getDeliveryTime(),
                order.getPreparationTimeMinutes(),
                itemResponses
        );
    }
}
