package com.example.ecommerce;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {"order-events", "product-events"})
public abstract class BaseIntegrationTest {

    private static RedisServer redisServer;
    protected static WireMockServer wireMockServer;

    @BeforeAll
    static void setupEmbeddedServices() throws Exception {
        // Start embedded Redis
        redisServer = new RedisServer(6379);
        redisServer.start();

        // Start WireMock
        wireMockServer = new WireMockServer(options().port(8090));
        wireMockServer.start();
    }

    @AfterAll
    static void tearDownEmbeddedServices() {
        if (redisServer != null) {
            redisServer.stop();
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
        registry.add("payment.api.url", () -> "http://localhost:8090");
    }
}
