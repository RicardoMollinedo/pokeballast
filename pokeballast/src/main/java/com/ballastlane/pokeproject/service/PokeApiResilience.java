package com.ballastlane.pokeproject.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.util.function.Supplier;

/**
 * Resilience4j protection for the calls to PokeAPI, applied in the recommended order:
 * {@code Retry( CircuitBreaker( RateLimiter( Bulkhead( call ))))}.
 * <ul>
 *   <li><b>Bulkhead</b>: caps concurrent calls (the US01 fan-out can produce up to 100 per page).</li>
 *   <li><b>RateLimiter</b>: caps calls per second sent to PokeAPI (fair use).</li>
 *   <li><b>CircuitBreaker</b>: after repeated transient failures, fails fast without calling PokeAPI.</li>
 *   <li><b>Retry</b>: retries transient failures (5xx, timeouts) with exponential backoff; never a 404.</li>
 * </ul>
 * Instances and limits come from {@code resilience4j.*} in application.yaml (instance name {@code pokeapi}).
 */
public record PokeApiResilience(CircuitBreaker circuitBreaker, Retry retry, RateLimiter rateLimiter, Bulkhead bulkhead) {

    public static PokeApiResilience of(String name, CircuitBreakerConfig circuitBreaker, RetryConfig retry,
                                       RateLimiterConfig rateLimiter, BulkheadConfig bulkhead) {
        return new PokeApiResilience(CircuitBreaker.of(name, circuitBreaker), Retry.of(name, retry),
                RateLimiter.of(name, rateLimiter), Bulkhead.of(name, bulkhead));
    }

    public <T> T execute(Supplier<T> call) {
        Supplier<T> decorated = Bulkhead.decorateSupplier(bulkhead, call);
        decorated = RateLimiter.decorateSupplier(rateLimiter, decorated);
        decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
        decorated = Retry.decorateSupplier(retry, decorated);
        return decorated.get();
    }
}
