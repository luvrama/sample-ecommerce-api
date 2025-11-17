# Integration Test Plan: ProductController.updateProduct

## Endpoint Details
- Controller: ProductController
- Method: updateProduct
- HTTP: PUT /api/products/{id}
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
  "name": "Updated Product",
  "price": 39.99,
  "stock": 50
}
```

### Field Validation Rules (from project_analysis.json)
- name: @NotBlank, @Size(min=2, max=100)
- price: @NotNull, @DecimalMin("0.01")
- stock: @Min(0)

### Invalid Request Examples for Testing
1. **Missing required field:**
   ```json
   {"price": 39.99, "stock": 50}  // Missing name → 400
   ```

2. **Invalid format:**
   ```json
   {"name": "A", "price": 39.99, "stock": 50}  // name too short → 400
   ```

3. **Out of range:**
   ```json
   {"name": "Updated", "price": -1.00, "stock": 50}  // negative price → 400
   ```

## Test Cases to Generate

### 1. Happy Path
- Test: updateProduct_ValidRequest_ReturnsUpdatedProduct
- Setup: Insert existing product in H2 database
- Request: PUT /api/products/1 with valid update
- Verify: Status 200, response contains updated product
- **Verify Side Effects:**
  - Query database to verify product was updated
  - Verify Redis cache was invalidated

### 2. Not Found
- Test: updateProduct_NonExistentId_Returns404
- Request: PUT /api/products/999 with valid data
- Verify: Status 404

### 3. Validation Errors
- Test: updateProduct_InvalidRequest_Returns400
- Examples: Missing name, invalid price, negative stock
- Verify: Status 400, no data updated in database

### 4. Service Integration
- Verify: ProductService.update() called
- Verify: ProductRepository.save() executed
- Verify: CacheService.invalidate() called

## Expected Test File
- Name: ProductControllerIntegrationTest.java
- Package: com.example.ecommerce.controller
- Extends: BaseIntegrationTest
- Test Methods: 5 methods