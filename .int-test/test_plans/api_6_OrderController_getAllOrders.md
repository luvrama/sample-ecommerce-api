# Integration Test Plan: OrderController.getAllOrders

## Endpoint Details
- Controller: OrderController
- Method: getAllOrders
- HTTP: GET /api/orders
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

## Test Cases to Generate

### 1. Happy Path
- Test: getAllOrders_WithData_ReturnsOrderList
- Setup: Insert test orders in H2 database
- Request: GET /api/orders
- Verify: Status 200, response contains order list
- **Verify Side Effects:**
  - Query database to verify data was retrieved correctly

### 2. Empty Database
- Test: getAllOrders_EmptyDatabase_ReturnsEmptyList
- Setup: Clean database
- Request: GET /api/orders
- Verify: Status 200, empty list response

### 3. Service Integration
- Verify: OrderService.findAll() called
- Verify: OrderRepository queried

## Expected Test File
- Name: OrderControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 3 methods