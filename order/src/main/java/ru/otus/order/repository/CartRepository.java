package ru.otus.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.order.model.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}