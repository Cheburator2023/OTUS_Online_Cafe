package ru.otus.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.order.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}