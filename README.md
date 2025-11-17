# Sample E-Commerce API

A Spring Boot REST API demonstrating **all integration test patterns** for the LLM-first test generator.

## Integration Patterns Demonstrated

### ✅ Database (H2)
- POST: Verify data persisted
- PUT: Verify data updated
- DELETE: Verify data removed
- Validation errors: Verify no data persisted

### ✅ Cache (Redis)
- GET: Verify data cached
- PUT: Verify cache invalidated

### ✅ External API (WireMock)
- POST: Verify API called once
- Verify request body contains correct data
- Error cases: Verify API NOT called

### ✅ Message Queue (Kafka)
- POST: Verify event published
- Verify message contains correct data

---

## Architecture

```
ProductController → ProductService → ProductRepository (H2)
                                   → CacheService (Redis)
                                   → EventPublisher (Kafka)

OrderController → OrderService → ProductService
                              → PaymentClient (WireMock)
                              → EventPublisher (Kafka)
                              → OrderRepository (H2)
```

---

## Endpoints

### Products
- `GET /api/products` - List all products
- `GET /api/products/{id}` - Get product (cached in Redis)
- `POST /api/products` - Create product (publishes Kafka event)
- `PUT /api/products/{id}` - Update product (invalidates cache)
- `DELETE /api/products/{id}` - Delete product

### Orders
- `GET /api/orders` - List all orders
- `GET /api/orders/{id}` - Get order by ID
- `POST /api/orders` - Create order (calls payment API, publishes Kafka event, updates stock)

---

## Running Tests

```bash
mvn test
```

**Embedded services start automatically:**
- H2 (in-memory)
- Redis (port 6379)
- Kafka (embedded broker)
- WireMock (port 8090)

---

## Test Examples

### Database Verification
```java
@Test
void testCreateProduct_SavesToDatabase() {
    ResponseEntity<Product> response = restTemplate.postForEntity(...);
    
    // Verify side effect
    Product saved = productRepository.findById(response.getBody().getId()).orElse(null);
    assertNotNull(saved, "Must be saved in database");
}
```

### Cache Verification
```java
@Test
void testGetProduct_CachesInRedis() {
    restTemplate.getForEntity("/api/products/" + id, Product.class);
    
    // Verify side effect
    Object cached = redisTemplate.opsForValue().get("product:" + id);
    assertNotNull(cached, "Must be cached in Redis");
}
```

### WireMock Verification
```java
@Test
void testCreateOrder_CallsPaymentAPI() {
    wireMockServer.stubFor(post("/payment").willReturn(ok()));
    
    restTemplate.postForEntity("/api/orders", orderRequest, Order.class);
    
    // Verify side effect
    wireMockServer.verify(1, postRequestedFor(urlEqualTo("/payment")));
}
```

### Kafka Verification
```java
@Test
void testCreateProduct_PublishesEvent() {
    KafkaConsumer<String, String> consumer = createConsumer();
    
    restTemplate.postForEntity("/api/products", request, Product.class);
    
    // Verify side effect
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
    assertEquals(1, records.count(), "Must publish 1 event");
}
```

---

## Dependencies

- Spring Boot 3.2.0
- H2 Database (embedded)
- Redis (embedded-redis 1.4.3 - ARM64 compatible)
- Kafka (spring-kafka-test)
- WireMock 3.3.1

---

## Use with LLM-First Analyzer v2

This project demonstrates all side-effect validation patterns:

1. **Prompt 1**: Discovers 8 endpoints with dependencies
2. **Prompt 2**: Generates BaseIntegrationTest with all embedded services
3. **Prompt 3**: Creates test plans with side-effect verification
4. **Prompt 4**: Validates coverage
5. **Prompt 5**: Generates tests that verify DB, cache, API calls, and events