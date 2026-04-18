package ru.otus.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Setter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Setter
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Setter
    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    @Setter
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Setter
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Setter
    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long userId, BigDecimal amount, OrderStatus status, LocalDateTime deliveryTime) {
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.deliveryTime = deliveryTime;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public enum OrderStatus {
        PENDING,          // ожидает подтверждения клиента (есть товары на готовку)
        CONFIRMED,        // подтверждён клиентом, средства списаны, товары и доставка зарезервированы
        PREPARING,        // готовится
        READY_FOR_DELIVERY,
        DELIVERED,        // курьер доставил, ожидает подтверждения клиента
        COMPLETED,        // клиент подтвердил получение
        FAILED,
        CANCELLED
    }
}