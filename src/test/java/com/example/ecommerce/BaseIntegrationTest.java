package com.example.ecommerce;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.junit.jupiter.api.BeforeEach;
import redis.embedded.RedisServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Base class for all integration tests.
 * 
 * Configured Services:
 * - H2 Database (embedded)
 * - Redis (embedded on port 6370)
 * - Kafka (embedded broker)
 * 
 * Usage:
 * class MyControllerIntegrationTest extends BaseIntegrationTest {
 *     @Test
 *     void testEndpoint() { ... }
 * }
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"product-events", "order-events"})
public abstract class BaseIntegrationTest {
    
    private static RedisServer redisServer;
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    protected DataSource dataSource;
    
    @BeforeAll
    static void startEmbeddedServices() throws Exception {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }
    
    @AfterAll
    static void stopEmbeddedServices() throws Exception {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    
    @BeforeEach
    void setupTest() throws Exception {
        cleanupRedis();
        cleanupDatabase();
    }
    
    protected void cleanupRedis() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Redis cleanup failed - continue with test
        }
    }
    
    protected void cleanupDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            try {
                stmt.execute("DELETE FROM orders");
            } catch (Exception e) {
                // Table may not exist yet
            }
            try {
                stmt.execute("DELETE FROM products");
            } catch (Exception e) {
                // Table may not exist yet
            }
        }
    }
}