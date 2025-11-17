package com.example.ecommerce;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BaseIntegrationTestVerification extends BaseIntegrationTest {
    
    @Test
    void verifyApplicationStarts() {
        assertThat(restTemplate).isNotNull();
    }
    
    @Test
    void verifyRedisEmbedded() {
        assertThat(redisTemplate).isNotNull();
        redisTemplate.opsForValue().set("test-key", "test-value");
        String value = (String) redisTemplate.opsForValue().get("test-key");
        assertThat(value).isEqualTo("test-value");
    }
    
    @Test
    void verifyDatabaseConnection() throws Exception {
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getConnection()).isNotNull();
    }
}