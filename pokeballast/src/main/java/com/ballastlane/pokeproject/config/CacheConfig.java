package com.ballastlane.pokeproject.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine in-process cache for PokeAPI responses (names, size and TTL in {@code spring.cache.*}).
 * A distributed cache would only be needed with several API instances.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {
}
