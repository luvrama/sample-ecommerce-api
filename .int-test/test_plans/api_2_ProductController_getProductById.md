# Integration Test Plan: ProductController.getProductById

## Endpoint Details
- Controller: ProductController
- Method: getProductById
- HTTP: GET /api/products/{id}
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
- Test: getProductById_ValidId_ReturnsProduct
- Setup: Insert test product in H2 database
- Request: GET /api/products/1
- Verify: Status 200, response contains product data
- **Verify Side Effects:**
  - Query Redis cache to verify data was cached

### 2. Not Found
- Test: getProductById_InvalidId_Returns404
- Request: GET /api/products/999
- Verify: Status 404

### 3. Service Integration
- Verify: ProductService.findById() called
- Verify: ProductRepository queried
- Verify: Redis cache checked and populated

## Expected Test File
- Name: ProductControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 3 methods