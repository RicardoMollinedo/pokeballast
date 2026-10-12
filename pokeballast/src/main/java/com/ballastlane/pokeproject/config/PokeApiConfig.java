package com.ballastlane.pokeproject.config;

import com.ballastlane.pokeproject.service.PokeApiResilience;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PokeApiConfig.PokeApiProperties.class)
public class PokeApiConfig {

    @Bean
    RestClient pokeApiRestClient(RestClient.Builder builder, PokeApiProperties properties) {
        // explicit timeouts: a slow upstream must never hang our request threads
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "pokeballast/1.0")
                .build();
    }

    /** Instances configured under resilience4j.*.instances.pokeapi in application.yaml. */
    @Bean
    PokeApiResilience pokeApiResilience(CircuitBreakerRegistry circuitBreakers, RetryRegistry retries,
                                        RateLimiterRegistry rateLimiters, BulkheadRegistry bulkheads) {
        String name = "pokeapi";
        return new PokeApiResilience(circuitBreakers.circuitBreaker(name), retries.retry(name),
                rateLimiters.rateLimiter(name), bulkheads.bulkhead(name));
    }

    /** Virtual threads: the US01 fan-out is pure I/O waiting, so one cheap thread per call is fine. */
    @Bean(destroyMethod = "close")
    ExecutorService pokeApiExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @ConfigurationProperties(prefix = "pokeapi")
    public record PokeApiProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
    }
}
