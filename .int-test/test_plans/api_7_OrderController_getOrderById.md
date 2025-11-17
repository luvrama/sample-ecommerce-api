# Integration Test Plan: OrderController.getOrderById

## Endpoint Details
- Controller: OrderController
- Method: getOrderById
- HTTP: GET /api/orders/{id}
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
- Test: getOrderById_ValidId_ReturnsOrder
- Setup: Insert test order in H2 database
- Request: GET /api/orders/1
- Verify: Status 200, response contains order data

### 2. Not Found
- Test: getOrderById_InvalidId_Returns404
- Request: GET /api/orders/999
- Verify: Status 404

### 3. Service Integration
- Verify: OrderService.findById() called
- Verify: OrderRepository queried

## Expected Test File
- Name: OrderControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 3 methods