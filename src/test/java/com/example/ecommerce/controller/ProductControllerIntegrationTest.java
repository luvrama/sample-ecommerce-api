package com.example.ecommerce.controller;

import com.example.ecommerce.BaseIntegrationTest;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.CacheService;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheService cacheService;

    @Test
    void testGetAllProducts_ReturnsProductList() {
        // Setup
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("29.99"));
        product1.setStock(10);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("39.99"));
        product2.setStock(5);
        productRepository.save(product2);

        // Execute
        ResponseEntity<Product[]> response = restTemplate.getForEntity("/api/products", Product[].class);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
    }

    @Test
    void testGetProductById_ValidId_ReturnsProductAndCaches() {
        // Setup
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("49.99"));
        product.setStock(15);
        Product saved = productRepository.save(product);

        // Execute
        ResponseEntity<Product> response = restTemplate.getForEntity("/api/products/" + saved.getId(), Product.class);

        // Verify HTTP response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Product", response.getBody().getName());

        // Verify side effect: Product cached in Redis
        Object cached = cacheService.getProduct(saved.getId());
        assertNotNull(cached, "Product should be cached in Redis");
    }

    @Test
    void testGetProductById_InvalidId_Returns404() {
        // Execute
        ResponseEntity<Product> response = restTemplate.getForEntity("/api/products/999", Product.class);

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testCreateProduct_ValidRequest_SavesAndPublishesEvent() {
        // Setup Kafka consumer
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // Setup request
        Product product = new Product();
        product.setName("New Product");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(20);

        // Execute
        ResponseEntity<Product> response = restTemplate.postForEntity("/api/products", product, Product.class);

        // Verify HTTP response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());

        // Verify side effect 1: Database save
        Product saved = productRepository.findById(response.getBody().getId()).orElse(null);
        assertNotNull(saved, "Product must be saved in database");
        assertEquals("New Product", saved.getName());
        assertEquals(new BigDecimal("99.99"), saved.getPrice());

        // Verify side effect 2: Kafka event published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertEquals(1, records.count(), "Must publish 1 event");
        ConsumerRecord<String, String> record = records.iterator().next();
        assertEquals("PRODUCT_CREATED", record.key());
        assertTrue(record.value().contains("\"productId\":" + saved.getId()));
        assertTrue(record.value().contains("\"name\":\"New Product\""));

        consumer.close();
    }

    @Test
    void testCreateProduct_InvalidRequest_NoSideEffects() {
        long countBefore = productRepository.count();

        // Setup Kafka consumer
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // Setup invalid request (blank name)
        Product invalid = new Product();
        invalid.setName("");
        invalid.setPrice(new BigDecimal("99.99"));
        invalid.setStock(20);

        // Execute
        ResponseEntity<String> response = restTemplate.postForEntity("/api/products", invalid, String.class);

        // Verify HTTP response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Verify side effect 1: No database save
        assertEquals(countBefore, productRepository.count(), "No product should be saved on validation error");

        // Verify side effect 2: No Kafka event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertEquals(0, records.count(), "No Kafka event should be published on validation error");

        consumer.close();
    }

    @Test
    void testUpdateProduct_ValidRequest_UpdatesAndInvalidatesCache() {
        // Setup
        Product product = new Product();
        product.setName("Original Product");
        product.setPrice(new BigDecimal("50.00"));
        product.setStock(10);
        Product saved = productRepository.save(product);

        // Cache the product first
        cacheService.cacheProduct(saved.getId(), saved);
        assertNotNull(cacheService.getProduct(saved.getId()), "Product should be cached initially");

        // Setup update request
        Product update = new Product();
        update.setName("Updated Product");
        update.setPrice(new BigDecimal("75.00"));
        update.setStock(15);

        // Execute
        restTemplate.put("/api/products/" + saved.getId(), update);

        // Verify side effect 1: Database updated
        Product updated = productRepository.findById(saved.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("Updated Product", updated.getName());
        assertEquals(new BigDecimal("75.00"), updated.getPrice());

        // Verify side effect 2: Cache invalidated
        Object cached = cacheService.getProduct(saved.getId());
        assertNull(cached, "Cache must be invalidated after update");
    }

    @Test
    void testDeleteProduct_ValidId_RemovesAndInvalidatesCache() {
        // Setup
        Product product = new Product();
        product.setName("To Delete");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(5);
        Product saved = productRepository.save(product);

        // Cache the product first
        cacheService.cacheProduct(saved.getId(), saved);

        // Execute
        restTemplate.delete("/api/products/" + saved.getId());

        // Verify side effect 1: Database delete
        assertFalse(productRepository.findById(saved.getId()).isPresent(), "Product should be deleted from database");

        // Verify side effect 2: Cache invalidated
        Object cached = cacheService.getProduct(saved.getId());
        assertNull(cached, "Cache must be invalidated after delete");
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("test", "true", embeddedKafka);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("product-events"));
        return consumer;
    }
}