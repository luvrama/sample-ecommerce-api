# Integration Test Plan: ProductController.deleteProduct

## Endpoint Details
- Controller: ProductController
- Method: deleteProduct
- HTTP: DELETE /api/products/{id}
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
- Test: deleteProduct_ValidId_ReturnsNoContent
- Setup: Insert existing product in H2 database
- Request: DELETE /api/products/1
- Verify: Status 204 (No Content)
- **Verify Side Effects:**
  - Query database to verify product was removed
  - Verify Redis cache was cleared

### 2. Not Found
- Test: deleteProduct_NonExistentId_Returns404
- Request: DELETE /api/products/999
- Verify: Status 404

### 3. Service Integration
- Verify: ProductService.delete() called
- Verify: ProductRepository.deleteById() executed
- Verify: CacheService.evict() called

## Expected Test File
- Name: ProductControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 3 methods