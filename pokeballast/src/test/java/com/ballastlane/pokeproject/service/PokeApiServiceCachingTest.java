package com.ballastlane.pokeproject.service;

import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import com.ballastlane.pokeproject.mapper.PokeApiMapper;
import com.ballastlane.pokeproject.util.PokeApiUrlUtil;
import com.ballastlane.pokeproject.util.TextNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Proves the cache sits in front of the HTTP call: the bl calls the service through its Spring proxy. */
@SpringJUnitConfig(PokeApiServiceCachingTest.Config.class)
class PokeApiServiceCachingTest {

    /** Injected through its port, exactly as the bl uses it. */
    @Autowired
    private PokemonCatalogPort service;
    @Autowired
    private MockRestServiceServer server;

    @Test
    void secondCallForTheSamePokemonIsServedFromTheCache() {
        server.expect(once(), requestTo("https://pokeapi.test/pokemon/bulbasaur"))
                .andRespond(withSuccess(new ClassPathResource("pokeapi/pokemon-1.json"), MediaType.APPLICATION_JSON));

        service.fetchPokemon("bulbasaur");
        service.fetchPokemon("bulbasaur");

        server.verify();
    }

    @Configuration
    @EnableCaching
    static class Config {

        private final RestClient.Builder builder = RestClient.builder().baseUrl("https://pokeapi.test");

        @Bean
        MockRestServiceServer server() {
            return MockRestServiceServer.bindTo(builder).build();
        }

        @Bean
        PokeApiService pokeApiService(MockRestServiceServer server) {
            return new PokeApiService(builder.build(), new PokeApiMapper(new PokeApiUrlUtil(), new TextNormalizer()),
                TestResilience.passThrough());
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
