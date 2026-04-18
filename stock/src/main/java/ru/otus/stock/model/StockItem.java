package ru.otus.stock.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_items")
@Getter
@NoArgsConstructor
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Setter
    @Column(nullable = false)
    private Integer quantity;

    @Setter
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Setter
    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes = 0;

    @Setter
    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;

    public StockItem(Long productId, Integer quantity, java.math.BigDecimal price, Integer preparationTimeMinutes) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.preparationTimeMinutes = preparationTimeMinutes;
    }
}