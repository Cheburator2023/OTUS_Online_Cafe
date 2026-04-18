package ru.otus.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Setter
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Setter
    @Column(nullable = false)
    private Long quantity;

    public CartItem(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}