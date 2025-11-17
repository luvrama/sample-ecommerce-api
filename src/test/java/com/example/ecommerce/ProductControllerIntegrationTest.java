package com.example.ecommerce;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testCreateProduct_ValidRequest_SavesToDatabase() {
        // Setup
        Product request = new Product();
        request.setName("Laptop");
        request.setPrice(new BigDecimal("999.99"));
        request.setStock(10);

        // Execute
        ResponseEntity<Product> response = restTemplate.postForEntity(
            "/api/products", request, Product.class);

        // Verify HTTP response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify side effect: Data persisted in database
        Product saved = productRepository.findById(response.getBody().getId()).orElse(null);
        assertNotNull(saved, "Product must be saved in database");
        assertEquals("Laptop", saved.getName());
        assertEquals(new BigDecimal("999.99"), saved.getPrice());
    }

    @Test
    void testCreateProduct_PublishesKafkaEvent() throws Exception {
        // Setup Kafka consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            "test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList("product-events"));

        // Setup request
        Product request = new Product();
        request.setName("Mouse");
        request.setPrice(new BigDecimal("29.99"));
        request.setStock(50);

        // Execute
        ResponseEntity<Product> response = restTemplate.postForEntity(
            "/api/products", request, Product.class);

        // Verify side effect: Kafka message published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertEquals(1, records.count(), "Must publish 1 event");

        ConsumerRecord<String, String> record = records.iterator().next();
        assertEquals("PRODUCT_CREATED", record.key());
        assertTrue(record.value().contains("\"productId\":" + response.getBody().getId()));
        assertTrue(record.value().contains("\"name\":\"Mouse\""));

        consumer.close();
    }

    @Test
    void testGetProduct_CachesInRedis() {
        // Setup
        Product product = new Product();
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("79.99"));
        product.setStock(20);
        Product saved = productRepository.save(product);

        // Execute
        restTemplate.getForEntity("/api/products/" + saved.getId(), Product.class);

        // Verify side effect: Cached in Redis
        Object cached = redisTemplate.opsForValue().get("product:" + saved.getId());
        assertNotNull(cached, "Product must be cached in Redis");
    }

    @Test
    void testUpdateProduct_InvalidatesCache() {
        // Setup
        Product product = new Product();
        product.setName("Monitor");
        product.setPrice(new BigDecimal("299.99"));
        product.setStock(5);
        Product saved = productRepository.save(product);
        
        // Cache the product
        redisTemplate.opsForValue().set("product:" + saved.getId(), saved);

        // Execute update
        Product updateRequest = new Product();
        updateRequest.setName("Monitor Updated");
        updateRequest.setPrice(new BigDecimal("279.99"));
        updateRequest.setStock(5);
        
        restTemplate.put("/api/products/" + saved.getId(), updateRequest);

        // Verify side effect: Cache invalidated
        Object cached = redisTemplate.opsForValue().get("product:" + saved.getId());
        assertNull(cached, "Cache must be invalidated after update");
    }

    @Test
    void testDeleteProduct_RemovesFromDatabase() {
        // Setup
        Product product = new Product();
        product.setName("Headphones");
        product.setPrice(new BigDecimal("149.99"));
        product.setStock(15);
        Product saved = productRepository.save(product);

        // Execute
        restTemplate.delete("/api/products/" + saved.getId());

        // Verify side effect: Removed from database
        assertFalse(productRepository.findById(saved.getId()).isPresent(),
            "Product must be deleted from database");
    }

    @Test
    void testCreateProduct_InvalidRequest_NoDataPersisted() {
        // Setup
        long countBefore = productRepository.count();
        Product invalid = new Product();
        invalid.setName("A"); // Too short
        invalid.setPrice(new BigDecimal("-10")); // Negative

        // Execute
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/products", invalid, String.class);

        // Verify HTTP response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Verify side effect: No data persisted
        assertEquals(countBefore, productRepository.count(),
            "No product should be saved on validation error");
    }
}
