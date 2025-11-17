package com.example.ecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payment.api.url:http://localhost:8090}")
    private String paymentApiUrl;

    public boolean processPayment(String customerEmail, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", customerEmail);
        request.put("amount", amount);
        request.put("currency", "USD");

        Map<String, Object> response = restTemplate.postForObject(
            paymentApiUrl + "/payment", request, Map.class);
        
        return "success".equals(response.get("status"));
    }
}
