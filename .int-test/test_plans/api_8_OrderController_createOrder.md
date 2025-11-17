# Integration Test Plan: OrderController.createOrder

## Endpoint Details
- Controller: OrderController
- Method: createOrder
- HTTP: POST /api/orders
- Package: com.example.ecommerce.controller

## Test Requirements
- Extend: BaseIntegrationTest
- Use: TestRestTemplate
- Services: H2, Redis, Kafka
- Framework: Spring Boot 3.2.0

## Source Files
Reference original source files (paths from project_analysis.json):
- Controller: src/main/java/com/example/ecommerce/controller/OrderController.java
- Services:
  - src/main/java/com/example/ecommerce/service/OrderService.java (implementation)
  - src/main/java/com/example/ecommerce/service/ProductService.java (implementation)
  - src/main/java/com/example/ecommerce/service/PaymentClient.java (implementation)
  - src/main/java/com/example/ecommerce/service/EventPublisher.java (implementation)
- Models:
  - src/main/java/com/example/ecommerce/model/Order.java
  - src/main/java/com/example/ecommerce/model/Product.java
- Repositories:
  - src/main/java/com/example/ecommerce/repository/OrderRepository.java (interface)
  - src/main/java/com/example/ecommerce/repository/ProductRepository.java (interface)

## Request Body

### Valid Request Example
```json
{
  "customerEmail": "customer@example.com",
  "productId": 1,
  "quantity": 2,
  "totalAmount": 59.98
}
```

### Field Validation Rules (from project_analysis.json)
- customerEmail: @NotBlank, @Email
- productId: @NotNull
- quantity: @Min(1)
- totalAmount: @NotNull

### Invalid Request Examples for Testing
1. **Missing required field:**
   ```json
   {"productId": 1, "quantity": 2, "totalAmount": 59.98}  // Missing customerEmail → 400
   ```

2. **Invalid format:**
   ```json
   {"customerEmail": "invalid-email", "productId": 1, "quantity": 2, "totalAmount": 59.98}  // Invalid email → 400
   ```

3. **Out of range:**
   ```json
   {"customerEmail": "test@example.com", "productId": 1, "quantity": 0, "totalAmount": 59.98}  // quantity < 1 → 400
   ```

## Test Cases to Generate

### 1. Happy Path
- Test: createOrder_ValidRequest_ReturnsCreatedOrder
- Setup: Insert test product in H2 database, configure WireMock payment API
- Request: POST /api/orders with valid order
- Verify: Status 201, response contains created order
- **Verify Side Effects:**
  - Query database to verify order was persisted
  - Verify PaymentClient called WireMock API once
  - Verify Kafka event was published to order-events topic
  - Verify product stock was updated

### 2. Validation Errors
- Test: createOrder_InvalidRequest_Returns400
- Examples: Missing email, invalid email format, zero quantity
- Verify: Status 400, no data persisted in database
- **Verify Side Effects:**
  - Verify PaymentClient NOT called
  - Verify no Kafka events published

### 3. Payment Failure
- Test: createOrder_PaymentFails_Returns500
- Setup: Configure WireMock to return payment error
- Request: POST /api/orders with valid order
- Verify: Status 500, no order persisted

### 4. Service Integration
- Verify: OrderService.save() called
- Verify: PaymentClient.processPayment() executed
- Verify: EventPublisher published order-created event
- Verify: ProductService.updateStock() called

## Expected Test File
- Name: OrderControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 5 methods