package com.example.ecommerce.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void publishOrderCreated(Long orderId, String customerEmail) {
        String message = String.format("{\"orderId\":%d,\"email\":\"%s\"}", orderId, customerEmail);
        kafkaTemplate.send("order-events", "ORDER_CREATED", message);
    }

    public void publishProductCreated(Long productId, String name) {
        String message = String.format("{\"productId\":%d,\"name\":\"%s\"}", productId, name);
        kafkaTemplate.send("product-events", "PRODUCT_CREATED", message);
    }
}
