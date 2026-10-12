package com.ballastlane.pokeproject.service;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.UUID;

/** Resilience that lets every call through once: for tests focused on mapping and error translation. */
final class TestResilience {

    private TestResilience() {
    }

    static PokeApiResilience passThrough() {
        return PokeApiResilience.of("test-" + UUID.randomUUID(),
                CircuitBreakerConfig.custom().minimumNumberOfCalls(1000).build(),
                RetryConfig.custom().maxAttempts(1).build(),
                RateLimiterConfig.custom().limitForPeriod(10_000).limitRefreshPeriod(Duration.ofSeconds(1)).build(),
                BulkheadConfig.custom().maxConcurrentCalls(1000).build());
    }
}
