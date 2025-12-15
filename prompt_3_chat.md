
---

# Prompt 3: Generate Test Plans

## Your Task

For each endpoint needing integration tests, generate test plans that reference original source code.

---

## Instructions

### Step 1: Read Previous Outputs

Read:
- `.int-test/01_analysis_README.md` - Get list of NEW endpoints
- `.int-test/02_base_infrastructure_README.md` - Get BaseIntegrationTest details

### Step 2: Filter Endpoints for Integration Test Generation

From `project_analysis.json`, get endpoints where integration tests are needed.

**CRITICAL: Generate integration tests for endpoints WITHOUT integration tests**

**Include endpoints with:**
- ✅ `status == &quot;NO_TEST&quot;` - no tests at all
- ✅ `status == &quot;HAS_UNIT_TEST_ONLY&quot;` - has unit test, needs integration test

**Skip endpoints with:**
- ❌ `status == &quot;HAS_INTEGRATION_TEST&quot;` - already has integration test

**Filtering Logic:**
```
FOR each endpoint in project_analysis.json:
  IF endpoint.status == &quot;NO_TEST&quot;:
    ✅ Include - no tests exist
    Log: &quot;Generate [HTTP_METHOD] [PATH] - No tests&quot;
    
  ELSE IF endpoint.status == &quot;HAS_UNIT_TEST_ONLY&quot;:
    ✅ Include - has unit test, needs integration test
    Log: &quot;Generate [HTTP_METHOD] [PATH] - Has unit test: [test_file]&quot;
    
  ELSE IF endpoint.status == &quot;HAS_INTEGRATION_TEST&quot;:
    ❌ Skip - already has integration test
    Log: &quot;Skip [HTTP_METHOD] [PATH] - Has integration test: [test_file]&quot;
```

**Rationale:** Unit tests (@WebMvcTest) test controller logic with mocked services. Integration tests (@SpringBootTest) test full workflow with real services/DB. Both are valuable and complementary.

**Example:**
```json
// Include - no tests
{
  &quot;path&quot;: &quot;/api/users/{id}&quot;,
  &quot;status&quot;: &quot;NO_TEST&quot;,
  &quot;existing_test&quot;: null
}
→ Generate integration test

// Include - has unit test only
{
  &quot;path&quot;: &quot;/api/orders&quot;,
  &quot;status&quot;: &quot;HAS_UNIT_TEST_ONLY&quot;,
  &quot;existing_test&quot;: &quot;OrderControllerTest.java&quot;,
  &quot;test_type&quot;: &quot;unit&quot;,
  &quot;test_annotation&quot;: &quot;@WebMvcTest&quot;
}
→ Generate integration test (complements existing unit test)

// Skip - has integration test
{
  &quot;path&quot;: &quot;/api/payments&quot;,
  &quot;status&quot;: &quot;HAS_INTEGRATION_TEST&quot;,
  &quot;existing_test&quot;: &quot;PaymentControllerIntegrationTest.java&quot;,
  &quot;test_type&quot;: &quot;integration&quot;,
  &quot;test_annotation&quot;: &quot;@SpringBootTest&quot;
}
→ Skip (integration test already exists)
```

### Step 3: Generate Test Plan for Each Endpoint

For each endpoint needing integration tests, create a test plan.

**Source file paths are already in `project_analysis.json` - reference them directly, don&#39;t copy.**

Create `.int-test/test_plans/api_[N]_[endpoint_name].md`

**Template:**
```markdown
# Integration Test Plan: [Controller].[method]

## Endpoint Details
- Controller: [ControllerName]
- Method: [methodName]
- HTTP: [GET/POST/PUT/DELETE] [/path]
- Package: [com.example.rest]

## Test Requirements
- Extend: BaseIntegrationTest
- Use: [TestRestTemplate/WebTestClient/MockMvc]
- Services: [MongoDB, Redis] (from Prompt 2)
- Framework: [Spring Boot/Micronaut/Quarkus]

## Source Files
Reference original source files (paths from project_analysis.json):
- Controller: src/main/java/.../[ControllerName].java
- Services (all levels):
  - src/main/java/.../[ServiceName].java (interface)
  - src/main/java/.../[ServiceNameImpl].java (implementation, if exists)
- Models: src/main/java/.../[ModelName].java
- Repositories (all levels):
  - src/main/java/.../[RepositoryName].java (interface)
  - Custom implementations (if any)
- Utilities/Mappers (if any):
  - src/main/java/.../[UtilityName].java

**Note:** Include ALL dependencies from project_analysis.json, regardless of depth.

## Request Body (for POST/PUT endpoints)

### Valid Request Example
```json
{
  &quot;email&quot;: &quot;test@example.com&quot;,
  &quot;name&quot;: &quot;Test User&quot;,
  &quot;age&quot;: 25
}
```

### Field Validation Rules (from project_analysis.json)
- email: @NotNull, @Email
- name: @NotNull, @Size(min=2, max=50)
- age: @Min(18), @Max(120)

### Invalid Request Examples for Testing
1. **Missing required field:**
   ```json
   {&quot;name&quot;: &quot;Test&quot;, &quot;age&quot;: 25}  // Missing email → 400
   ```

2. **Invalid format:**
   ```json
   {&quot;email&quot;: &quot;invalid&quot;, &quot;name&quot;: &quot;Test&quot;, &quot;age&quot;: 25}  // Invalid email → 400
   ```

3. **Out of range:**
   ```json
   {&quot;email&quot;: &quot;test@example.com&quot;, &quot;name&quot;: &quot;Test&quot;, &quot;age&quot;: 15}  // age &lt; 18 → 400
   ```

## Test Cases to Generate

### 1. Happy Path
- Test: Valid request returns 200 OK
- Setup: Create test data in database/cache
- Verify: Response contains expected data
- **Verify Side Effects:**
  - **POST/PUT:** Query database/cache to verify data was persisted
  - **DELETE:** Query database/cache to verify data was removed
  - **External API calls:** Verify WireMock was called with expected parameters
  - **Message queue:** Verify message was published to Kafka/RabbitMQ

### 2. Validation Errors
- Test: Invalid request returns 400 Bad Request
- Examples: Missing required fields, invalid format
- Verify: No data persisted (database remains unchanged)

### 3. Not Found
- Test: Non-existent resource returns 404 Not Found
- Verify: No side effects occurred

### 4. Security (if endpoint is secured)

**Check project_analysis.json for security info:**
- If `security.required == true` → Generate security tests
- If `security == null` → Skip security tests

**Test cases:**
- Test: Unauthorized - No credentials returns 401
  - No Authorization header
  - Expected: 401 Unauthorized

- Test: Forbidden - Insufficient permissions returns 403
  - Valid credentials but wrong role
  - Example: USER role accessing ADMIN endpoint
  - Expected: 403 Forbidden
  - Use: `@WithMockUser(roles = &quot;USER&quot;)`

- Test: Authorized - Valid credentials returns 200
  - Correct role
  - Example: ADMIN role accessing ADMIN endpoint
  - Expected: 200 OK
  - Use: `@WithMockUser(roles = &quot;ADMIN&quot;)`

**Security annotation examples:**
```java
// From project_analysis.json
&quot;security&quot;: {
  &quot;annotation&quot;: &quot;@PreAuthorize(\&quot;hasRole(&#39;ADMIN&#39;)\&quot;)&quot;,
  &quot;roles&quot;: [&quot;ADMIN&quot;]
}

// Generate tests with:
@Test
@WithMockUser(roles = &quot;USER&quot;)
void testEndpoint_InsufficientRole_Returns403() { }

@Test
@WithMockUser(roles = &quot;ADMIN&quot;)
void testEndpoint_ValidRole_Returns200() { }
```

### 5. Service Integration
- Verify: Service methods called correctly
- Verify: Repository interactions work
- Verify: [Redis/Kafka] integration if applicable

## Expected Test File
- Name: [ControllerName]IntegrationTest.java
- Package: [com.example.rest]
- Extends: BaseIntegrationTest
- Test Methods: ~3-7 methods (more if secured)
```

### Step 5: Create Summary

List all test plans created with statistics.

---

## Output

### Create: `.int-test/03_test_plans_README.md`

```markdown
# Prompt 3: Test Plans Generated - Results

## Summary
Generated integration test plans for [N] endpoints. Skipped [M] endpoints that already have integration tests.

## Test Plans Created
1. `test_plans/api_1_UserController_getUser.md`
   - Endpoint: GET /api/users/{id}
   - Status: NO_TEST
   - Source: src/main/java/.../UserController.java
   - Dependencies: 3 services, 2 models, 1 repository

2. `test_plans/api_2_UserController_createUser.md`
   - Endpoint: POST /api/users
   - Status: HAS_UNIT_TEST_ONLY (has @WebMvcTest, generating integration test)
   - Source: src/main/java/.../UserController.java
   - Dependencies: 3 services, 3 models, 1 repository

[... list all plans ...]

## Source Files Referenced
All source file paths from project_analysis.json:

Controllers: [count]
├─ src/main/java/.../UserController.java
└─ src/main/java/.../OrderController.java

Services: [count]
├─ src/main/java/.../UserService.java
├─ src/main/java/.../OrderService.java
└─ src/main/java/.../NotificationService.java

Models: [count]
├─ src/main/java/.../User.java
├─ src/main/java/.../UserDTO.java
└─ src/main/java/.../CreateUserRequest.java

Repositories: [count]
├─ src/main/java/.../UserRepository.java
└─ src/main/java/.../OrderRepository.java

## Skipped (Already Have Integration Tests)
1. POST /api/auth/login
   - Test: AuthControllerIntegrationTest.java
   - Type: integration (@SpringBootTest)
   - Reason: Integration test already exists

[... list all skipped ...]

## Generated (Need Integration Tests)
Endpoints with NO_TEST or HAS_UNIT_TEST_ONLY status:

1. GET /api/users/{id}
   - Status: NO_TEST
   - Reason: No tests exist

2. POST /api/orders
   - Status: HAS_UNIT_TEST_ONLY
   - Unit test: OrderControllerTest.java (@WebMvcTest)
   - Reason: Has unit test, generating complementary integration test

[... list all generated ...]

## Statistics
- Total endpoints: [total]
- NO_TEST: [count]
- HAS_UNIT_TEST_ONLY: [count]
- HAS_INTEGRATION_TEST: [count]
- Integration test plans generated: [N]
- Skipped (has integration test): [M]
- Integration test coverage: [percentage]%

## Validation Checklist
- [x] All NO_TEST endpoints have test plans
- [x] All HAS_UNIT_TEST_ONLY endpoints have test plans
- [x] Source file paths recorded
- [x] Dependencies identified (3 levels)
- [x] Only HAS_INTEGRATION_TEST endpoints skipped
- [x] Test plans reference BaseIntegrationTest

## Status
✅ PASSED - Ready for validation

## Next Steps
Prompt 4 should:
1. Read this README
2. Verify 100% coverage for NEW endpoints
3. Check all dependencies included
4. Generate validation report
```

### Directory Structure

```
.int-test/
├── test_plans/
│   ├── api_1_UserController_getUser.md
│   ├── api_2_UserController_createUser.md
│   └── api_3_OrderController_getOrder.md
└── 03_test_plans_README.md
```

**Note:** Source files remain in original locations. Test plans reference paths from project_analysis.json.

---

## Validation

Before finishing, verify:

- [ ] `.int-test/03_test_plans_README.md` created
- [ ] All endpoints needing tests have plans
- [ ] Source file paths recorded
- [ ] Request body examples included (for POST/PUT)
- [ ] Security test cases included (if secured)
- [ ] Dependencies identified (services, models, repos)
- [ ] Only HAS_INTEGRATION_TEST endpoints skipped
- [ ] Count matches: endpoints needing tests = test plans
- [ ] README shows ✅ PASSED or ⚠️ PARTIAL SUCCESS

## Error Recovery

If this step fails:
1. Check error in `.int-test/03_test_plans_README.md`
2. Common issues:
   - **Source files not found:** Verify paths in project_analysis.json
   - **Request body validation missing:** Generate basic tests without detailed validation
   - **Security info missing:** Skip security test cases
3. **Fallback Strategy:**
   - Generate test plans for successfully analyzed endpoints
   - Skip endpoints with missing dependencies
   - Mark status: ⚠️ PARTIAL SUCCESS
   - **Proceed to Prompt 4** with available test plans

**Output validation message:**
```
✅ Prompt 3 Complete

Test Plans Generated: [N]
├─ api_1_UserController_getUser.md
├─ api_2_UserController_createUser.md
└─ ... ([N] total)

Source Files Referenced: [total]
├─ Controllers: [count]
├─ Services: [count]
├─ Models: [count]
└─ Repositories: [count]

Skipped (Has Integration Test): [M]
├─ POST /api/auth/login (integration: @SpringBootTest)
└─ ... ([M] total)

Generated (Need Integration Test): [N]
├─ GET /api/users/{id} (NO_TEST)
├─ POST /api/orders (HAS_UNIT_TEST_ONLY: @WebMvcTest)
└─ ... ([N] total)

Coverage for NEW Endpoints: 100%

Output:
├─ .int-test/sources/ ([total] files)
├─ .int-test/test_plans/ ([N] plans)
└─ .int-test/03_test_plans_README.md

Status: ✅ READY FOR PROMPT 4
Next: Run Prompt 4 to validate coverage
```

---

## Example Test Plan

**File:** `.int-test/test_plans/api_1_UserController_getUser.md`

```markdown
# Integration Test Plan: UserController.getUser

## Endpoint Details
- Controller: UserController
- Method: getUser
- HTTP: GET /api/users/{id}
- Package: com.example.rest

## Test Requirements
- Extend: BaseIntegrationTest
- Use: TestRestTemplate
- Services: MongoDB, Redis
- Framework: Spring Boot 3.2.0

## Source Files
Reference original source files (paths from project_analysis.json):
- Controller: src/main/java/com/example/rest/UserController.java
- Services:
  - src/main/java/com/example/service/UserService.java (interface)
  - src/main/java/com/example/service/UserServiceImpl.java (implementation)
- Models:
  - src/main/java/com/example/model/User.java
- Repositories:
  - src/main/java/com/example/repository/UserRepository.java (interface)

## Test Cases to Generate

### 1. Happy Path
- Test: getUser_ValidId_ReturnsUser
- Setup: Insert test user in MongoDB
- Request: GET /api/users/123
- Verify: Status 200, response contains user data

### 2. Not Found
- Test: getUser_InvalidId_Returns404
- Request: GET /api/users/999
- Verify: Status 404

### 3. Service Integration
- Verify: UserService.findById() called
- Verify: UserRepository queried
- Verify: Redis cache checked

## Expected Test File
- Name: UserControllerIntegrationTest.java
- Package: com.example.rest
- Extends: BaseIntegrationTest
- Test Methods: 3 methods
```

export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home &amp;&amp; /opt/homebrew/bin/mvn -U test -Dtest=EmbeddedServicesVerificationTest -Dtrust_all_cert=true -Dcom.sun.net.ssl.checkRevocation=false -Dtrust_all_cert=true

makesure to use tail -20 or more to reduce the logs generated by maven commands

---

I'll execute Prompt 3 to generate test plans for endpoints needing integration tests. Let me start by reading the previous outputs and analyzing the project structure.

---

Now I'll create the test plans directory and generate test plans for each endpoint that needs integration tests.

---

Creating the test_plans directory to store individual test plan files

---

shell

---

Now I'll generate test plans for each endpoint. Based on the analysis, all 8 endpoints have NO_TEST status and need integration tests.

---

Creating test plan for GET /api/products endpoint

---

Creating test plan for GET /api/products/{id} endpoint

---

Creating test plan for POST /api/products endpoint

---

Creating test plan for PUT /api/products/{id} endpoint

---

Creating test plan for DELETE /api/products/{id} endpoint

---

Creating test plan for GET /api/orders endpoint

---

Creating test plan for GET /api/orders/{id} endpoint

---

Creating test plan for POST /api/orders endpoint

---

Now I'll create the summary README file with all the test plans and statistics.

---

Creating the summary README file with all test plan details and statistics

---

Let me verify the directory structure and files created:

---

Perfect! Let me provide a final validation summary:

---

Counting the test plan files to verify all 8 endpoints have test plans

---

shell

---


## ✅ Prompt 3 Complete

**Test Plans Generated: 8**
├─ api_1_ProductController_getAllProducts.md
├─ api_2_ProductController_getProductById.md
├─ api_3_ProductController_createProduct.md
├─ api_4_ProductController_updateProduct.md
├─ api_5_ProductController_deleteProduct.md
├─ api_6_OrderController_getAllOrders.md
├─ api_7_OrderController_getOrderById.md
└─ api_8_OrderController_createOrder.md

**Source Files Referenced: 11**
├─ Controllers: 2
├─ Services: 5
├─ Models: 2
└─ Repositories: 2

**Skipped (Has Integration Test): 0**
- No existing integration tests found

**Generated (Need Integration Test): 8**
├─ GET /api/products (NO_TEST)
├─ GET /api/products/{id} (NO_TEST)
├─ POST /api/products (NO_TEST)
├─ PUT /api/products/{id} (NO_TEST)
├─ DELETE /api/products/{id} (NO_TEST)
├─ GET /api/orders (NO_TEST)
├─ GET /api/orders/{id} (NO_TEST)
└─ POST /api/orders (NO_TEST)

**Coverage for NEW Endpoints: 100%**

**Output:**
├─ .int-test/test_plans/ (8 plans)
└─ .int-test/03_test_plans_README.md

**Status: ✅ READY FOR PROMPT 4**
**Next: Run Prompt 4 to validate coverage**

All test plans include:
- Complete source file references from project_analysis.json
- Request body examples with validation rules for POST/PUT endpoints
- Side-effect verification (database, cache, Kafka, WireMock)
- Comprehensive test cases (happy path, validation errors, not found)
- Integration with BaseIntegrationTest infrastructure
