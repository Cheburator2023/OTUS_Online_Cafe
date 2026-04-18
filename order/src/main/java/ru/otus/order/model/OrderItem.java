package ru.otus.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Setter
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Setter
    @Column(nullable = false)
    private Long quantity;

    @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    public OrderItem(Long productId, Long quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
}