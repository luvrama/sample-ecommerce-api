# Integration Test Plan: ProductController.createProduct

## Endpoint Details
- Controller: ProductController
- Method: createProduct
- HTTP: POST /api/products
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

## Request Body

### Valid Request Example
```json
{
  "name": "Test Product",
  "price": 29.99,
  "stock": 100
}
```

### Field Validation Rules (from project_analysis.json)
- name: @NotBlank, @Size(min=2, max=100)
- price: @NotNull, @DecimalMin("0.01")
- stock: @Min(0)

### Invalid Request Examples for Testing
1. **Missing required field:**
   ```json
   {"price": 29.99, "stock": 100}  // Missing name → 400
   ```

2. **Invalid format:**
   ```json
   {"name": "", "price": 29.99, "stock": 100}  // Blank name → 400
   ```

3. **Out of range:**
   ```json
   {"name": "Test", "price": 0.00, "stock": 100}  // price < 0.01 → 400
   ```

## Test Cases to Generate

### 1. Happy Path
- Test: createProduct_ValidRequest_ReturnsCreatedProduct
- Setup: Clean database
- Request: POST /api/products with valid product
- Verify: Status 201, response contains created product
- **Verify Side Effects:**
  - Query database to verify product was persisted
  - Verify Kafka event was published to product-events topic

### 2. Validation Errors
- Test: createProduct_InvalidRequest_Returns400
- Examples: Missing name, invalid price, negative stock
- Verify: Status 400, no data persisted in database

### 3. Service Integration
- Verify: ProductService.save() called
- Verify: ProductRepository.save() executed
- Verify: EventPublisher published product-created event

## Expected Test File
- Name: ProductControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 4 methods