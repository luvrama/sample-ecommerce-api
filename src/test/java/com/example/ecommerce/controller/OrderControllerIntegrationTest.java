package com.example.ecommerce.controller;

import com.example.ecommerce.BaseIntegrationTest;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "payment.api.url=http://localhost:8090"
})
class OrderControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setupWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8090));
        wireMockServer.start();
    }

    @AfterEach
    void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testGetAllOrders_ReturnsOrderList() {
        // Setup
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("50.00"));
        product.setStock(10);
        Product savedProduct = productRepository.save(product);

        Order order1 = new Order();
        order1.setCustomerEmail("customer1@example.com");
        order1.setProductId(savedProduct.getId());
        order1.setQuantity(1);
        order1.setTotalAmount(new BigDecimal("50.00"));
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setCustomerEmail("customer2@example.com");
        order2.setProductId(savedProduct.getId());
        order2.setQuantity(2);
        order2.setTotalAmount(new BigDecimal("100.00"));
        orderRepository.save(order2);

        // Execute
        ResponseEntity<Order[]> response = restTemplate.getForEntity("/api/orders", Order[].class);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
    }

    @Test
    void testGetOrderById_ValidId_ReturnsOrder() {
        // Setup
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("30.00"));
        product.setStock(5);
        Product savedProduct = productRepository.save(product);

        Order order = new Order();
        order.setCustomerEmail("test@example.com");
        order.setProductId(savedProduct.getId());
        order.setQuantity(1);
        order.setTotalAmount(new BigDecimal("30.00"));
        Order saved = orderRepository.save(order);

        // Execute
        ResponseEntity<Order> response = restTemplate.getForEntity("/api/orders/" + saved.getId(), Order.class);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getCustomerEmail());
    }

    @Test
    void testGetOrderById_InvalidId_Returns500() {
        // Execute
        ResponseEntity<Order> response = restTemplate.getForEntity("/api/orders/999", Order.class);

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testCreateOrder_ValidRequest_AllSideEffects() {
        // Setup WireMock for payment API
        wireMockServer.stubFor(post(urlEqualTo("/payment"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"success\"}")));

        // Setup Kafka consumer
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // Setup product
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(10);
        Product savedProduct = productRepository.save(product);

        // Setup order request
        Order order = new Order();
        order.setCustomerEmail("customer@example.com");
        order.setProductId(savedProduct.getId());
        order.setQuantity(2);
        order.setTotalAmount(new BigDecimal("50.00"));

        // Execute
        ResponseEntity<Order> response = restTemplate.postForEntity("/api/orders", order, Order.class);

        // Verify HTTP response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());

        // Verify side effect 1: Order saved in database
        Order savedOrder = orderRepository.findById(response.getBody().getId()).orElse(null);
        assertNotNull(savedOrder, "Order must be saved in database");
        assertEquals("customer@example.com", savedOrder.getCustomerEmail());
        assertEquals(new BigDecimal("50.00"), savedOrder.getTotalAmount());

        // Verify side effect 2: Payment API called
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/payment"))
            .withRequestBody(matchingJsonPath("$.email", equalTo("customer@example.com")))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("50.0"))));

        // Verify side effect 3: Product stock updated
        Product updatedProduct = productRepository.findById(savedProduct.getId()).orElse(null);
        assertNotNull(updatedProduct);
        assertEquals(8, updatedProduct.getStock(), "Product stock should be reduced by order quantity");

        // Verify side effect 4: Kafka event published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertEquals(1, records.count(), "Must publish 1 order event");
        ConsumerRecord<String, String> record = records.iterator().next();
        assertEquals("ORDER_CREATED", record.key());
        assertTrue(record.value().contains("\"orderId\":" + savedOrder.getId()));
        assertTrue(record.value().contains("\"email\":\"customer@example.com\""));

        consumer.close();
    }

    @Test
    void testCreateOrder_InvalidRequest_NoSideEffects() {
        long orderCountBefore = orderRepository.count();

        // Setup Kafka consumer
        KafkaConsumer<String, String> consumer = createKafkaConsumer();

        // Setup invalid order (invalid email)
        Order invalid = new Order();
        invalid.setCustomerEmail("invalid-email");
        invalid.setProductId(1L);
        invalid.setQuantity(1);

        // Execute
        ResponseEntity<String> response = restTemplate.postForEntity("/api/orders", invalid, String.class);

        // Verify HTTP response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Verify side effect 1: No order saved
        assertEquals(orderCountBefore, orderRepository.count(), "No order should be saved on validation error");

        // Verify side effect 2: No payment API called
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/payment")));

        // Verify side effect 3: No Kafka event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
        assertEquals(0, records.count(), "No Kafka event should be published on validation error");

        consumer.close();
    }

    @Test
    void testCreateOrder_PaymentFails_NoOrderSaved() {
        // Setup WireMock for payment failure
        wireMockServer.stubFor(post(urlEqualTo("/payment"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"failed\"}")));

        long orderCountBefore = orderRepository.count();

        // Setup product
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("40.00"));
        product.setStock(5);
        Product savedProduct = productRepository.save(product);

        // Setup order request
        Order order = new Order();
        order.setCustomerEmail("customer@example.com");
        order.setProductId(savedProduct.getId());
        order.setQuantity(1);
        order.setTotalAmount(new BigDecimal("40.00"));

        // Execute
        ResponseEntity<String> response = restTemplate.postForEntity("/api/orders", order, String.class);

        // Verify HTTP response (payment failure should cause 500)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        // Verify side effect 1: Payment API was called
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/payment")));

        // Verify side effect 2: No order saved due to payment failure
        assertEquals(orderCountBefore, orderRepository.count(), "No order should be saved when payment fails");

        // Verify side effect 3: Product stock unchanged
        Product unchangedProduct = productRepository.findById(savedProduct.getId()).orElse(null);
        assertNotNull(unchangedProduct);
        assertEquals(5, unchangedProduct.getStock(), "Product stock should remain unchanged when payment fails");
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("test", "true", embeddedKafka);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("order-events"));
        return consumer;
    }
}