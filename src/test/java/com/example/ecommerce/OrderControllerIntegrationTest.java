package com.example.ecommerce;

import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class OrderControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testCreateOrder_CallsPaymentAPI() {
        // Setup WireMock stub
        wireMockServer.stubFor(post("/payment")
            .willReturn(ok().withBody("{\"status\":\"success\"}")));

        // Setup product
        Product product = new Product();
        product.setName("Laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setStock(10);
        Product saved = productRepository.save(product);

        // Setup order request
        Order orderRequest = new Order();
        orderRequest.setCustomerEmail("test@example.com");
        orderRequest.setProductId(saved.getId());
        orderRequest.setQuantity(2);

        // Execute
        ResponseEntity<Order> response = restTemplate.postForEntity(
            "/api/orders", orderRequest, Order.class);

        // Verify HTTP response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Verify side effect: WireMock called once
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/payment")));
    }

    @Test
    void testCreateOrder_SendsCorrectPaymentAmount() {
        // Setup WireMock stub
        wireMockServer.stubFor(post("/payment")
            .willReturn(ok().withBody("{\"status\":\"success\"}")));

        // Setup product
        Product product = new Product();
        product.setName("Mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setStock(50);
        Product saved = productRepository.save(product);

        // Setup order request
        Order orderRequest = new Order();
        orderRequest.setCustomerEmail("buyer@example.com");
        orderRequest.setProductId(saved.getId());
        orderRequest.setQuantity(3);

        // Execute
        restTemplate.postForEntity("/api/orders", orderRequest, Order.class);

        // Verify side effect: Correct amount sent to payment API
        wireMockServer.verify(postRequestedFor(urlEqualTo("/payment"))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("89.97")))
            .withRequestBody(matchingJsonPath("$.currency", equalTo("USD")))
            .withRequestBody(matchingJsonPath("$.email", equalTo("buyer@example.com"))));
    }

    @Test
    void testCreateOrder_SavesToDatabase() {
        // Setup WireMock stub
        wireMockServer.stubFor(post("/payment")
            .willReturn(ok().withBody("{\"status\":\"success\"}")));

        // Setup product
        Product product = new Product();
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("79.99"));
        product.setStock(20);
        Product saved = productRepository.save(product);

        // Setup order request
        Order orderRequest = new Order();
        orderRequest.setCustomerEmail("customer@example.com");
        orderRequest.setProductId(saved.getId());
        orderRequest.setQuantity(1);

        // Execute
        ResponseEntity<Order> response = restTemplate.postForEntity(
            "/api/orders", orderRequest, Order.class);

        // Verify side effect: Order saved in database
        Order savedOrder = orderRepository.findById(response.getBody().getId()).orElse(null);
        assertNotNull(savedOrder, "Order must be saved in database");
        assertEquals("customer@example.com", savedOrder.getCustomerEmail());
        assertEquals(new BigDecimal("79.99"), savedOrder.getTotalAmount());
    }

    @Test
    void testCreateOrder_InsufficientStock_NoPaymentCall() {
        // Setup WireMock stub
        wireMockServer.stubFor(post("/payment")
            .willReturn(ok().withBody("{\"status\":\"success\"}")));

        // Setup product with low stock
        Product product = new Product();
        product.setName("Monitor");
        product.setPrice(new BigDecimal("299.99"));
        product.setStock(1);
        Product saved = productRepository.save(product);

        // Setup order request exceeding stock
        Order orderRequest = new Order();
        orderRequest.setCustomerEmail("test@example.com");
        orderRequest.setProductId(saved.getId());
        orderRequest.setQuantity(5);

        // Execute
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/orders", orderRequest, String.class);

        // Verify HTTP response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        // Verify side effect: Payment API NOT called
        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/payment")));
    }

    @Test
    void testCreateOrder_UpdatesProductStock() {
        // Setup WireMock stub
        wireMockServer.stubFor(post("/payment")
            .willReturn(ok().withBody("{\"status\":\"success\"}")));

        // Setup product
        Product product = new Product();
        product.setName("Headphones");
        product.setPrice(new BigDecimal("149.99"));
        product.setStock(10);
        Product saved = productRepository.save(product);

        // Setup order request
        Order orderRequest = new Order();
        orderRequest.setCustomerEmail("test@example.com");
        orderRequest.setProductId(saved.getId());
        orderRequest.setQuantity(3);

        // Execute
        restTemplate.postForEntity("/api/orders", orderRequest, Order.class);

        // Verify side effect: Stock updated in database
        Product updated = productRepository.findById(saved.getId()).get();
        assertEquals(7, updated.getStock(), "Stock must be reduced by order quantity");
    }
}
