# Integration Test Plan: ProductController.getAllProducts

## Endpoint Details
- Controller: ProductController
- Method: getAllProducts
- HTTP: GET /api/products
- Package: com.example.ecommerce.controller

## Test Requirements
- Extend: BaseIntegrationTest
- Use: TestRestTemplate
- Services: H2, Redis, Kafka
- Framework: Spring Boot 3.2.0

## Source Files
Reference original source files (paths from project_analysis.json):
- Controller: src/main/java/com/example/ecommerce/controller/ProductController.java
- Services:
  - src/main/java/com/example/ecommerce/service/ProductService.java (implementation)
  - src/main/java/com/example/ecommerce/service/CacheService.java (implementation)
  - src/main/java/com/example/ecommerce/service/EventPublisher.java (implementation)
- Models:
  - src/main/java/com/example/ecommerce/model/Product.java
- Repositories:
  - src/main/java/com/example/ecommerce/repository/ProductRepository.java (interface)

## Test Cases to Generate

### 1. Happy Path
- Test: getAllProducts_WithData_ReturnsProductList
- Setup: Insert test products in H2 database
- Request: GET /api/products
- Verify: Status 200, response contains product list
- **Verify Side Effects:**
  - Query database to verify data was retrieved correctly

### 2. Empty Database
- Test: getAllProducts_EmptyDatabase_ReturnsEmptyList
- Setup: Clean database
- Request: GET /api/products
- Verify: Status 200, empty list response

### 3. Service Integration
- Verify: ProductService.findAll() called
- Verify: ProductRepository queried
- Verify: Cache interaction if applicable

## Expected Test File
- Name: ProductControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 3 methods