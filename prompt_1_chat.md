<div style="display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 200px 0 20px 0;">

<div style="font-size: 24px; margin-bottom: 12px;"><strong>Amazon Q</strong></div>
<div style="background: rgba(128, 128, 128, 0.15); border: 1px solid rgba(128, 128, 128, 0.25); border-radius: 8px; padding: 8px; margin: 4px 0; text-align: center;">
<div style="font-size: 14px; margin-bottom: 4px;"><strong>Did you know?</strong></div>
<div>Compact your conversation with /compact</div>
</div>

Select code & ask me to explain, debug or optimize it, or type `/` for quick actions

</div>

---

# Prompt 1: Analyze Project for Integration Testing

## Your Task

Analyze this Spring Boot project to identify REST endpoints, existing tests, and dependencies needed for integration test generation.

**Project Location:** current folder

---

## Instructions

### Step 1: Detect Framework &amp; Build Tool

1. Check for `pom.xml` or `build.gradle` files (including submodules)
2. Identify:
   - Framework: Spring Boot, Micronaut, or Quarkus
   - Version
   - Build tool: Maven or Gradle
   - Multi-module: Yes/No

### Step 2: Detect Services

Scan all `pom.xml` or `build.gradle` files for:
- MongoDB: `spring-boot-starter-data-mongodb` or `mongodb`
- Redis: `spring-boot-starter-data-redis` or `redis`
- Kafka: `spring-kafka`
- H2/JPA: `spring-boot-starter-data-jpa`

### Step 3: Find REST Endpoints

1. Scan directories: `**/rest/**`, `**/controller/**`, `**/api/**`, `**/resource/**`
2. For each Java file, detect:
   - Controller annotations: `@RestController`, `@Controller`, `@Path`
   - Method annotations: `@GetMapping`, `@PostMapping`, `@RequestMapping(method=GET)`, `@GET`, `@POST`
3. Extract:
   - HTTP method (GET, POST, PUT, DELETE)
   - Path/URL
   - Method name
   - Controller class
   - **Path parameters:** `@GetMapping(&quot;/users/{id}&quot;)` → extract `id` and its type
   - **Query parameters:** `@RequestParam String name`
   - **Request body:** `@RequestBody CreateUserRequest request`

4. **Extract parameter types:**
   ```java
   @GetMapping(&quot;/users/{id}&quot;)
   public User getUser(@PathVariable Long id) { }
   // → id: Long, location: path
   
   @PostMapping(&quot;/users&quot;)
   public User createUser(@RequestBody CreateUserRequest request) { }
   // → request: CreateUserRequest, location: body
   ```

5. **Extract validation annotations from request bodies:**
   ```java
   public class CreateUserRequest {
       @NotNull @Email
       private String email;
       
       @NotNull @Size(min=2, max=50)
       private String name;
       
       @Min(18) @Max(120)
       private Integer age;
   }
   ```

### Step 4: Identify Existing Tests (PRECISE)

**CRITICAL: The annotation determines test type, NOT the testing tool (MockMvc/TestRestTemplate)**

#### Integration Test Annotations (Full Context)
- `@SpringBootTest` - loads FULL application context
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` - real HTTP server
- `@SpringBootTest(webEnvironment = MOCK)` - mocked servlet
- `@QuarkusTest`, `@QuarkusIntegrationTest`
- `@MicronautTest`

#### Unit Test Slice Annotations (Isolated Layers)
**Web Layer:**
- `@WebMvcTest` - Spring MVC controllers only
- `@WebFluxTest` - Reactive WebFlux controllers
- `@GraphQlTest` - GraphQL endpoints

**Data Layer:**
- `@DataJpaTest` - JPA repositories
- `@DataJdbcTest`, `@JdbcTest` - JDBC
- `@DataMongoTest` - MongoDB
- `@DataNeo4jTest` - Neo4j
- `@DataRedisTest` - Redis
- `@DataLdapTest` - LDAP
- `@DataCassandraTest` - Cassandra
- `@DataCouchbaseTest` - Couchbase
- `@DataElasticsearchTest` - Elasticsearch
- `@DataR2dbcTest` - R2DBC

**Other:**
- `@RestClientTest` - REST clients
- `@JsonTest` - JSON serialization

#### Detection Algorithm

For each endpoint found:

1. **Find test files** in `src/test/java` matching controller name
2. **Parse test class annotations:**
   - If `@SpringBootTest` → INTEGRATION test
   - If slice annotation (e.g., `@WebMvcTest`) → UNIT test
   - Both types count as EXISTING tests
3. **Parse @Test methods** - look for test invocations:
   ```java
   // Integration tests
   mockMvc.perform(get(&quot;/v1/endpoint&quot;))
   webTestClient.get().uri(&quot;/v1/endpoint&quot;)
   restTemplate.getForEntity(&quot;/v1/endpoint&quot;, ...)
   
   // Unit tests
   mockMvc.perform(get(&quot;/v1/endpoint&quot;))  // with @WebMvcTest
   ```
4. **Match: HTTP method + path**
   - Extract method: `get()`, `post()`, `put()`, `delete()`
   - Extract path: `&quot;/v1/endpoint&quot;` or `uri(&quot;/v1/endpoint&quot;)`
5. **Mark endpoint status:**
   - `HAS_INTEGRATION_TEST` - if @SpringBootTest test found
   - `HAS_UNIT_TEST_ONLY` - if only slice test found (e.g., @WebMvcTest)
   - `NO_TEST` - if no test found
6. **Record test details:**
   ```json
   {
     &quot;existing_test&quot;: &quot;UserControllerTest.java&quot;,
     &quot;test_method&quot;: &quot;testCreateUser_Success&quot;,
     &quot;test_type&quot;: &quot;unit&quot;,  // or &quot;integration&quot;
     &quot;test_annotation&quot;: &quot;@WebMvcTest&quot;  // or &quot;@SpringBootTest&quot;
   }
   ```

**Integration Test Generation Logic:**
- ✅ Generate if status = `NO_TEST` or `HAS_UNIT_TEST_ONLY`
- ❌ Skip if status = `HAS_INTEGRATION_TEST`

**Rationale:** Unit tests (@WebMvcTest) and integration tests (@SpringBootTest) are COMPLEMENTARY. Unit tests verify controller logic with mocked services (fast). Integration tests verify full workflow with real services/DB (confidence). An endpoint should have BOTH.

**Example:**
```java
// This is a UNIT test (web layer only, mocked services)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockBean
    private UserService userService;  // ← Mocked
    
    @Test
    void testGetUser() {
        mockMvc.perform(get(&quot;/api/users/1&quot;))  // ← Match this
    }
}
// Status: HAS_UNIT_TEST_ONLY → Generate integration test

// This is an INTEGRATION test (full context, real services)
@SpringBootTest
class UserControllerIntegrationTest {
    @Autowired
    private UserService userService;  // ← Real service
    
    @Test
    void testGetUser() {
        mockMvc.perform(get(&quot;/api/users/1&quot;))  // ← Match this
    }
}
// Status: HAS_INTEGRATION_TEST → Skip generation
```

### Step 5: Map Dependencies (Complete Traversal)

For each REST controller, traverse ALL dependencies until no new dependencies found.

**Dependency Detection Rules:**

**Services:**
- `@Autowired` fields: `@Autowired private UserService userService;`
- Constructor injection: `public UserController(UserService userService)`
- Setter injection: `@Autowired public void setUserService(...)`

**Models/DTOs:**
- Method parameters: `public User createUser(CreateUserRequest request)`
- Return types: `public ResponseEntity&lt;User&gt; getUser()`
- Field types in services: `private User currentUser;`

**Repositories:**
- Extends Spring Data: `interface UserRepository extends JpaRepository`
- `@Autowired` in services

**Stop Conditions:**
- Framework classes (`org.springframework.*`, `javax.*`)
- Java standard library (`java.util.*`, `java.lang.*`)
- Already visited (circular dependency)

**Circular Dependency Algorithm:**
```python
visited = set()
dependencies = []
queue = [(controller, 0)]  # (class, level)

while queue:
    current, level = queue.pop(0)
    
    if current in visited:
        continue  # Skip circular reference
    
    visited.add(current)
    dependencies.append({&quot;class&quot;: current, &quot;level&quot;: level})
    
    deps = find_dependencies(current)
    for dep in deps:
        if dep not in visited:
            queue.append((dep, level + 1))
```

**Example:**
```
Controller (level 0)
├─ OrderService (level 1, interface)
├─ OrderServiceImpl (level 1, implementation)
│  ├─ OrderRepository (level 2, interface)
│  ├─ PaymentService (level 2, interface)
│  │  ├─ PaymentClient (level 3)
│  │  └─ PaymentValidator (level 3)
│  └─ NotificationService (level 2, interface)
│     └─ KafkaProducer (level 3)
└─ OrderMapper (level 1)
   └─ DateUtil (level 2)
```

### Step 6: Detect Security Configuration

Check for security setup:

**Spring Security:**
- `@EnableWebSecurity` in configuration
- `SecurityFilterChain` bean
- `@PreAuthorize`, `@Secured`, `@RolesAllowed` on endpoints

**JWT:**
- JWT dependencies: `jjwt`, `java-jwt`
- `JwtTokenProvider`, `JwtAuthenticationFilter` classes

**OAuth2:**
- `@EnableOAuth2Client`, `@EnableResourceServer`
- OAuth2 properties in application.yml

**Record secured endpoints:**
```java
@PreAuthorize(&quot;hasRole(&#39;ADMIN&#39;)&quot;)
@GetMapping(&quot;/admin/users&quot;)
// → Requires ADMIN role
```

### Step 7: Check Existing Test Infrastructure

Look for:
- `BaseIntegrationTest.java` or similar
- `application-test.properties` or `application-test.yml`
- Embedded service configurations

---

## Output

### Create: `.int-test/project_analysis.json`

```json
{
  &quot;project&quot;: {
    &quot;name&quot;: &quot;project-name&quot;,
    &quot;framework&quot;: &quot;spring-boot&quot;,
    &quot;version&quot;: &quot;3.2.0&quot;,
    &quot;build_tool&quot;: &quot;gradle&quot;,
    &quot;multi_module&quot;: false
  },
  &quot;services&quot;: {
    &quot;detected&quot;: [&quot;mongodb&quot;, &quot;redis&quot;],
    &quot;reactive&quot;: false
  },
  &quot;security&quot;: {
    &quot;enabled&quot;: true,
    &quot;type&quot;: &quot;spring-security&quot;,
    &quot;auth_method&quot;: &quot;jwt&quot;,
    &quot;secured_endpoints_count&quot;: 3
  },
  &quot;existing_test_infrastructure&quot;: {
    &quot;base_test_class&quot;: &quot;BaseIntegrationTest.java&quot;,
    &quot;base_test_path&quot;: &quot;src/test/java/.../BaseIntegrationTest.java&quot;,
    &quot;test_profile&quot;: &quot;application-test.properties&quot;,
    &quot;embedded_services&quot;: [&quot;mongodb&quot;]
  },
  &quot;rest_endpoints&quot;: [
    {
      &quot;controller&quot;: &quot;UserController.java&quot;,
      &quot;controller_path&quot;: &quot;src/main/java/.../UserController.java&quot;,
      &quot;package&quot;: &quot;com.example.rest&quot;,
      &quot;methods&quot;: [
        {
          &quot;name&quot;: &quot;getUser&quot;,
          &quot;http_method&quot;: &quot;GET&quot;,
          &quot;path&quot;: &quot;/api/users/{id}&quot;,
          &quot;parameters&quot;: [
            {
              &quot;name&quot;: &quot;id&quot;,
              &quot;type&quot;: &quot;Long&quot;,
              &quot;location&quot;: &quot;path&quot;,
              &quot;required&quot;: true
            }
          ],
          &quot;security&quot;: null,
          &quot;status&quot;: &quot;NO_TEST&quot;,
          &quot;existing_test&quot;: null
        },
        {
          &quot;name&quot;: &quot;createUser&quot;,
          &quot;http_method&quot;: &quot;POST&quot;,
          &quot;path&quot;: &quot;/api/users&quot;,
          &quot;parameters&quot;: [
            {
              &quot;name&quot;: &quot;request&quot;,
              &quot;type&quot;: &quot;CreateUserRequest&quot;,
              &quot;location&quot;: &quot;body&quot;,
              &quot;required&quot;: true,
              &quot;validation&quot;: {
                &quot;email&quot;: [&quot;@NotNull&quot;, &quot;@Email&quot;],
                &quot;name&quot;: [&quot;@NotNull&quot;, &quot;@Size(min=2, max=50)&quot;],
                &quot;age&quot;: [&quot;@Min(18)&quot;, &quot;@Max(120)&quot;]
              }
            }
          ],
          &quot;security&quot;: null,
          &quot;status&quot;: &quot;HAS_UNIT_TEST_ONLY&quot;,
          &quot;existing_test&quot;: &quot;UserControllerTest.java&quot;,
          &quot;test_method&quot;: &quot;testCreateUser_Success&quot;,
          &quot;test_type&quot;: &quot;unit&quot;,
          &quot;test_annotation&quot;: &quot;@WebMvcTest&quot;
        },
        {
          &quot;name&quot;: &quot;deleteUser&quot;,
          &quot;http_method&quot;: &quot;DELETE&quot;,
          &quot;path&quot;: &quot;/api/users/{id}&quot;,
          &quot;parameters&quot;: [
            {
              &quot;name&quot;: &quot;id&quot;,
              &quot;type&quot;: &quot;Long&quot;,
              &quot;location&quot;: &quot;path&quot;,
              &quot;required&quot;: true
            }
          ],
          &quot;security&quot;: {
            &quot;required&quot;: true,
            &quot;annotation&quot;: &quot;@PreAuthorize(\&quot;hasRole(&#39;ADMIN&#39;)\&quot;)&quot;,
            &quot;roles&quot;: [&quot;ADMIN&quot;]
          },
          &quot;status&quot;: &quot;HAS_INTEGRATION_TEST&quot;,
          &quot;existing_test&quot;: &quot;UserControllerIntegrationTest.java&quot;,
          &quot;test_method&quot;: &quot;testDeleteUser&quot;,
          &quot;test_type&quot;: &quot;integration&quot;,
          &quot;test_annotation&quot;: &quot;@SpringBootTest&quot;
        }
      ],
      &quot;dependencies&quot;: {
        &quot;services&quot;: [
          {
            &quot;class&quot;: &quot;UserService&quot;,
            &quot;path&quot;: &quot;src/main/java/.../UserService.java&quot;,
            &quot;type&quot;: &quot;interface&quot;,
            &quot;level&quot;: 1
          },
          {
            &quot;class&quot;: &quot;UserServiceImpl&quot;,
            &quot;path&quot;: &quot;src/main/java/.../UserServiceImpl.java&quot;,
            &quot;type&quot;: &quot;implementation&quot;,
            &quot;level&quot;: 1
          },
          {
            &quot;class&quot;: &quot;NotificationService&quot;,
            &quot;path&quot;: &quot;src/main/java/.../NotificationService.java&quot;,
            &quot;type&quot;: &quot;interface&quot;,
            &quot;level&quot;: 2,
            &quot;called_by&quot;: &quot;UserServiceImpl&quot;
          }
        ],
        &quot;models&quot;: [
          {
            &quot;class&quot;: &quot;User&quot;,
            &quot;path&quot;: &quot;src/main/java/.../User.java&quot;,
            &quot;usage&quot;: &quot;return_type&quot;
          }
        ],
        &quot;repositories&quot;: [
          {
            &quot;class&quot;: &quot;UserRepository&quot;,
            &quot;path&quot;: &quot;src/main/java/.../UserRepository.java&quot;,
            &quot;type&quot;: &quot;interface&quot;,
            &quot;level&quot;: 2
          }
        ],
        &quot;utilities&quot;: [
          {
            &quot;class&quot;: &quot;DateUtil&quot;,
            &quot;path&quot;: &quot;src/main/java/.../DateUtil.java&quot;,
            &quot;level&quot;: 3,
            &quot;called_by&quot;: &quot;UserServiceImpl&quot;
          }
        ],
        &quot;max_depth&quot;: 3
      }
    }
  ],
  &quot;statistics&quot;: {
    &quot;total_endpoints&quot;: 10,
    &quot;no_test&quot;: 4,
    &quot;has_unit_test_only&quot;: 4,
    &quot;has_integration_test&quot;: 2,
    &quot;integration_tests_needed&quot;: 8,
    &quot;coverage_percentage&quot;: 20.0
  }
}
```

### Create: `.int-test/01_analysis_README.md`

```markdown
# Prompt 1: Project Analysis - Results

## Summary
Analyzed [project-name] and identified REST endpoints, existing tests, and dependencies.

## Project Details
- Framework: Spring Boot 3.2.0
- Build Tool: Gradle
- Multi-Module: No
- Services: MongoDB, Redis

## REST Endpoints Found
- Total: 10 endpoints
- NO_TEST (need integration tests): 4
- HAS_UNIT_TEST_ONLY (need integration tests): 4
- HAS_INTEGRATION_TEST (skip): 2
- Integration test coverage: 20%

## Test Status Breakdown
- ✅ Generate integration test: 8 endpoints (NO_TEST + HAS_UNIT_TEST_ONLY)
- ❌ Skip (already has integration test): 2 endpoints

## Existing Test Infrastructure
- BaseIntegrationTest: ❌ Not found
- Test Profile: ❌ Not found
- Embedded Services: ❌ None configured

## Created Files
- `.int-test/project_analysis.json`

## Validation Checklist
- [x] project_analysis.json created
- [x] All endpoints identified
- [x] Existing tests precisely matched
- [x] Dependencies mapped (3 levels)
- [x] Statistics calculated

## Status
✅ PASSED

## Next Steps
Prompt 2 should:
1. Create BaseIntegrationTest.java
2. Configure embedded services: MongoDB, Redis
3. Create application-test.properties
4. Run verification test
```

---

## Validation

Before finishing, verify:

- [ ] `.int-test/` folder created
- [ ] `project_analysis.json` exists and is valid JSON
- [ ] `01_analysis_README.md` created
- [ ] All REST endpoints listed
- [ ] Parameters and validation extracted
- [ ] Security configuration detected
- [ ] NEW vs EXISTING correctly identified
- [ ] Statistics match counts
- [ ] README shows ✅ PASSED or ⚠️ PARTIAL SUCCESS

## Error Recovery

If this step fails:
1. Check error details in `.int-test/01_analysis_README.md`
2. Common issues:
   - **Multi-module project:** Ensure all pom.xml/build.gradle scanned
   - **No endpoints found:** Verify controller package names
   - **Dependency mapping incomplete:** Some deep dependencies may be missed
   - **Security detection failed:** Continue without security info
3. **Fallback Strategy:**
   - Mark status: ⚠️ PARTIAL SUCCESS
   - List what was successfully detected
   - Note what failed in README
   - **Proceed to Prompt 2** with available data

**Output validation message:**
```
✅ Prompt 1 Complete

Project: [name]
Framework: [framework] [version]
Build Tool: [maven/gradle]

Endpoints: [total] found
├─ NEW: [count]
├─ EXISTING: [count]
└─ Coverage: [percentage]%

Services: [count] detected
├─ [service1]
└─ [service2]

Output:
├─ .int-test/project_analysis.json
└─ .int-test/01_analysis_README.md

Status: ✅ READY FOR PROMPT 2
```

---

I'll analyze the Spring Boot project to identify REST endpoints, existing tests, and dependencies for integration test generation.

---

Now let me check the build configuration and dependencies:

---

Now let me examine the REST controllers to identify endpoints:

---

Now let me examine the model classes to understand validation annotations:

---

Now let me examine the service classes to map dependencies:

---

Let me examine the remaining service classes and repositories:

---

Now let me check the repositories:

---

Now let me check for existing tests:

---

Let me check the application properties and config:

---

Now let me create the analysis directory and files:

---

Creating the .int-test directory for analysis output

---

shell

---

Creating the project analysis JSON file with all detected endpoints, dependencies, and test status

---

Creating the analysis README with summary and validation results

---


✅ **Prompt 1 Complete**

**Project:** ecommerce-api  
**Framework:** Spring Boot 3.2.0  
**Build Tool:** Maven  

**Endpoints:** 8 found  
├─ NEW: 8  
├─ EXISTING: 0  
└─ Coverage: 0%  

**Services:** 3 detected  
├─ H2 Database  
├─ Redis Cache  
└─ Kafka Events  

**Output:**  
├─ .int-test/project_analysis.json  
└─ .int-test/01_analysis_README.md  

**Status:** ✅ READY FOR PROMPT 2