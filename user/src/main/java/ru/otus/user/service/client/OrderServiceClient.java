package ru.otus.user.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.order.url:http://order-app:8003}")
    private String orderServiceUrl;

    public void createCart(Long userId) {
        String url = orderServiceUrl + "/api/v1/carts?userId=" + userId;
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to create cart for userId: {}, status: {}", userId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling order service to create cart for userId: {}", userId, e);
        }
    }
}