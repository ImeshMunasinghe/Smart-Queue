package com.smartqueue;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Test-only Spring configuration that replaces RedisTemplate with a Mockito mock.
 * This allows @SpringBootTest integration tests to run without a live Redis instance.
 *
 * The RateLimitFilter already has fail-open logic (if Redis throws, the request proceeds),
 * so mocking the template is safe for functional correctness tests.
 */
@TestConfiguration
public class MockRedisConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> mockTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, String> mockOps = Mockito.mock(ValueOperations.class);

        // INCR returns 1 for every call (below rate limit) — test requests always pass
        Mockito.when(mockTemplate.opsForValue()).thenReturn(mockOps);
        Mockito.when(mockOps.increment(Mockito.anyString())).thenReturn(1L);
        Mockito.when(mockTemplate.expire(Mockito.anyString(), Mockito.any())).thenReturn(true);

        return mockTemplate;
    }
}
