package ru.otus.order.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.otus.order.dto.DeliverySlotResponse;
import ru.otus.order.dto.ReleaseDeliveryRequest;
import ru.otus.order.dto.ReserveDeliveryRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.delivery.url:http://delivery-service:8005}")
    private String deliveryServiceUrl;

    public List<DeliverySlotResponse> findNearestSlots(LocalDateTime afterTime, int limit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = afterTime.format(formatter);
        String url = deliveryServiceUrl + "/api/v1/delivery/slots/nearest?afterTime=" + formattedTime + "&limit=" + limit;
        ResponseEntity<List<DeliverySlotResponse>> response = restTemplate.exchange(
                url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    public void reserve(Long orderId, LocalDateTime deliveryTime) {
        String url = deliveryServiceUrl + "/api/v1/delivery/reserve";
        ReserveDeliveryRequest request = new ReserveDeliveryRequest(orderId, deliveryTime);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Delivery reservation failed");
        }
    }

    public void release(Long orderId) {
        String url = deliveryServiceUrl + "/api/v1/delivery/release";
        ReleaseDeliveryRequest request = new ReleaseDeliveryRequest(orderId);
        restTemplate.postForEntity(url, request, Void.class);
    }
}