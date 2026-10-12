package com.ballastlane.pokeproject.service;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokeApiMapper;
import com.ballastlane.pokeproject.util.PokeApiUrlUtil;
import com.ballastlane.pokeproject.util.TextNormalizer;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Each resilience mechanism around the PokeAPI calls, with small limits so the behaviour is visible. */
class PokeApiResilienceTest {

    private static final String BASE = "https://pokeapi.test/api/v2";
    private static final String BULBASAUR = BASE + "/pokemon/bulbasaur";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private PokeApiService serviceWith(PokeApiResilience resilience) {
        return new PokeApiService(builder.build(), new PokeApiMapper(new PokeApiUrlUtil(), new TextNormalizer()), resilience);
    }

    private static RetryConfig retry(int attempts) {
        return RetryConfig.custom().maxAttempts(attempts).waitDuration(Duration.ofMillis(10))
                .retryExceptions(RestClientException.class).ignoreExceptions(BallastException.class).build();
    }

    private static CircuitBreakerConfig lenientBreaker() {
        return CircuitBreakerConfig.custom().minimumNumberOfCalls(100).build();
    }

    private static RateLimiterConfig generousLimiter() {
        return RateLimiterConfig.custom().limitForPeriod(1000).limitRefreshPeriod(Duration.ofSeconds(1)).build();
    }

    private static BulkheadConfig wideBulkhead() {
        return BulkheadConfig.custom().maxConcurrentCalls(100).build();
    }

    private void respondOk() {
        server.expect(once(), requestTo(BULBASAUR))
                .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-1.json"), MediaType.APPLICATION_JSON));
    }

    @Test
    void transientFailuresAreRetriedWithBackoff() {
        server.expect(times(2), requestTo(BULBASAUR)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        respondOk();
        PokeApiService service = serviceWith(PokeApiResilience.of("t1", lenientBreaker(), retry(3), generousLimiter(), wideBulkhead()));

        assertThat(service.fetchPokemon("bulbasaur").name()).isEqualTo("bulbasaur");
        server.verify();
    }

    @Test
    void notFoundIsNeverRetried() {
        server.expect(once(), requestTo(BASE + "/pokemon/missingno")).andRespond(withResourceNotFound());
        PokeApiService service = serviceWith(PokeApiResilience.of("t2", lenientBreaker(), retry(3), generousLimiter(), wideBulkhead()));

        assertThatThrownBy(() -> service.fetchPokemon("missingno"))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEMON_NOT_FOUND.code()));
        server.verify();
    }

    @Test
    void exhaustedRetriesBecomePokeApiUnavailable() {
        server.expect(times(2), requestTo(BULBASAUR)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        PokeApiService service = serviceWith(PokeApiResilience.of("t3", lenientBreaker(), retry(2), generousLimiter(), wideBulkhead()));

        assertThatThrownBy(() -> service.fetchPokemon("bulbasaur"))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEAPI_UNAVAILABLE.code()));
        server.verify();
    }

    @Test
    void openCircuitFailsFastWithoutCallingPokeApi() {
        CircuitBreakerConfig breaker = CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .recordExceptions(RestClientException.class).ignoreExceptions(BallastException.class).build();
        server.expect(times(2), requestTo(BULBASAUR)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        PokeApiService service = serviceWith(PokeApiResilience.of("t4", breaker, retry(1), generousLimiter(), wideBulkhead()));

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.fetchPokemon("bulbasaur")).isInstanceOf(BallastException.class);
        }
        // third call: the circuit is open, so PokeAPI is not called at all (the mock server allows only 2 calls)
        assertThatThrownBy(() -> service.fetchPokemon("bulbasaur"))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEAPI_UNAVAILABLE.code()));
        server.verify();
    }

    @Test
    void notFoundDoesNotOpenTheCircuit() {
        CircuitBreakerConfig breaker = CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(50)
                .recordExceptions(RestClientException.class).ignoreExceptions(BallastException.class).build();
        server.expect(times(2), requestTo(BASE + "/pokemon/missingno")).andRespond(withResourceNotFound());
        respondOk();
        PokeApiService service = serviceWith(PokeApiResilience.of("t5", breaker, retry(1), generousLimiter(), wideBulkhead()));

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.fetchPokemon("missingno")).isInstanceOf(BallastException.class);
        }
        assertThat(service.fetchPokemon("bulbasaur").id()).as("a 404 is the caller's problem, not an outage").isEqualTo(1);
        server.verify();
    }

    @Test
    void rateLimiterCapsTheCallsSentToPokeApi() {
        RateLimiterConfig onePerMinute = RateLimiterConfig.custom()
                .limitForPeriod(1).limitRefreshPeriod(Duration.ofMinutes(1)).timeoutDuration(Duration.ZERO).build();
        respondOk();
        PokeApiService service = serviceWith(PokeApiResilience.of("t6", lenientBreaker(), retry(1), onePerMinute, wideBulkhead()));

        service.fetchPokemon("bulbasaur");
        assertThatThrownBy(() -> service.fetchPokemon("bulbasaur"))
                .as("second call in the same period is refused locally, PokeAPI never sees it")
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEAPI_UNAVAILABLE.code()));
        server.verify();
    }

    @Test
    void bulkheadRejectsCallsBeyondTheConcurrencyLimit() {
        BulkheadConfig noRoom = BulkheadConfig.custom().maxConcurrentCalls(0).maxWaitDuration(Duration.ZERO).build();
        PokeApiService service = serviceWith(PokeApiResilience.of("t7", lenientBreaker(), retry(1), generousLimiter(), noRoom));

        assertThatThrownBy(() -> service.fetchPokemon("bulbasaur"))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEAPI_UNAVAILABLE.code()));
        server.verify(); // no request reached PokeAPI
    }
}
