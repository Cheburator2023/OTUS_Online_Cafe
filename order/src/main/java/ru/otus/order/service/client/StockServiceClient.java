package ru.otus.order.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.otus.order.dto.ReleaseStockRequest;
import ru.otus.order.dto.ReserveStockRequest;
import ru.otus.order.dto.StockInfoResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.stock.url:http://stock-service:8004}")
    private String stockServiceUrl;

    public StockInfoResponse getProductInfo(Long productId, Long quantity) {
        String url = stockServiceUrl + "/api/v1/stock/products/" + productId + "/info?quantity=" + quantity;
        try {
            ResponseEntity<StockInfoResponse> response = restTemplate.getForEntity(url, StockInfoResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get product info for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Stock service error", e);
        }
    }

    public void reserve(Long productId, Long quantity, Long orderId) {
        String url = stockServiceUrl + "/api/v1/stock/reserve";
        ReserveStockRequest request = new ReserveStockRequest(orderId, productId, quantity);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Stock reservation failed");
        }
    }

    public void release(Long productId, Long quantity, Long orderId) {
        String url = stockServiceUrl + "/api/v1/stock/release";
        ReleaseStockRequest request = new ReleaseStockRequest(orderId, productId, quantity);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            log.error("Stock release failed for order {}: {}", request.orderId(), e.getMessage());
        }
    }

    public void commitReservation(Long productId, Long quantity) {
        String url = stockServiceUrl + "/api/v1/stock/commit?productId=" + productId + "&quantity=" + quantity;
        restTemplate.postForEntity(url, null, Void.class);
    }
}