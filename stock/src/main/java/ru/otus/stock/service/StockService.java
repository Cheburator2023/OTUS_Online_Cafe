package ru.otus.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.stock.dto.ProductInfoResponse;
import ru.otus.stock.dto.ReleaseRequest;
import ru.otus.stock.dto.ReserveRequest;
import ru.otus.stock.dto.StockResponse;
import ru.otus.stock.exception.InsufficientStockException;
import ru.otus.stock.exception.ProductNotFoundException;
import ru.otus.stock.model.StockItem;
import ru.otus.stock.repository.StockItemRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockItemRepository stockRepository;

    @Transactional(readOnly = true)
    public ProductInfoResponse getProductInfo(Long productId, Long requestedQuantity) {
        StockItem item = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        int available = item.getQuantity() - item.getReservedQuantity();
        // Товар доступен сейчас только если есть достаточное количество И не требует готовки
        boolean availableNow = (available >= requestedQuantity) && (item.getPreparationTimeMinutes() == 0);
        return new ProductInfoResponse(
                productId,
                requestedQuantity,
                availableNow,
                item.getPreparationTimeMinutes() != null ? item.getPreparationTimeMinutes() : 0,
                item.getPrice()
        );
    }

    @Transactional
    public StockResponse reserve(ReserveRequest request) {
        StockItem item = stockRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + request.productId()));

        int available = item.getQuantity() - item.getReservedQuantity();
        if (available < request.quantity()) {
            if (item.getPreparationTimeMinutes() > 0) {
                item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
                stockRepository.save(item);
                log.info("Reserved {} units of product {} with preparation time {} minutes for order {}",
                        request.quantity(), request.productId(), item.getPreparationTimeMinutes(), request.orderId());
                return new StockResponse(true, "Reserved with waiting, preparation time: " + item.getPreparationTimeMinutes());
            } else {
                throw new InsufficientStockException(
                        "Insufficient stock for product " + request.productId() +
                                ". Available: " + available + ", requested: " + request.quantity());
            }
        }

        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
        stockRepository.save(item);
        log.info("Reserved {} units of product {} for order {}", request.quantity(), request.productId(), request.orderId());
        return new StockResponse(true, "Reserved successfully");
    }

    @Transactional
    public void release(ReleaseRequest request) {
        StockItem item = stockRepository.findByProductId(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + request.productId()));
        int newReserved = Math.max(0, item.getReservedQuantity() - request.quantity());
        item.setReservedQuantity(newReserved);
        stockRepository.save(item);
        log.info("Released {} units of product {} for order {}", request.quantity(), request.productId(), request.orderId());
    }

    @Transactional
    public void commitReservation(Long productId, Long quantity) {
        StockItem item = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

        // Для товаров, требующих готовки, не уменьшаем фактический остаток, только резерв
        if (item.getPreparationTimeMinutes() > 0) {
            int newReserved = Math.max(0, item.getReservedQuantity() - quantity.intValue());
            item.setReservedQuantity(newReserved);
            stockRepository.save(item);
            log.info("Committed reservation for product {} (requires cooking), new reserved quantity: {}", productId, newReserved);
        } else {
            int newQuantity = item.getQuantity() - quantity.intValue();
            if (newQuantity < 0) {
                throw new RuntimeException("Cannot commit more than available");
            }
            item.setQuantity(newQuantity);
            item.setReservedQuantity(item.getReservedQuantity() - quantity.intValue());
            stockRepository.save(item);
            log.info("Committed {} units of product {}, new quantity: {}", quantity, productId, newQuantity);
        }
    }
}