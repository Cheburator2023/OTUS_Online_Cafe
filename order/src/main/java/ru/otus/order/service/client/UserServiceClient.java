package ru.otus.order.service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.otus.order.dto.UserResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user.url:http://user-app:8000}")
    private String userServiceUrl;

    public String getUserEmail(Long userId) {
        String url = userServiceUrl + "/api/v1/users/" + userId;
        try {
            UserResponse response = restTemplate.getForObject(url, UserResponse.class);
            return response != null ? response.email() : null;
        } catch (Exception e) {
            log.error("Failed to get user email for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }
}