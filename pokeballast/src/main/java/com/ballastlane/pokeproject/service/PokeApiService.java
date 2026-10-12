package com.ballastlane.pokeproject.service;

import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.NamePage;
import com.ballastlane.pokeproject.bl.model.PokemonData;
import com.ballastlane.pokeproject.bl.model.SpeciesData;
import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import com.ballastlane.pokeproject.dto.pokeapi.PokeApiDto;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokeApiMapper;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * External PokeAPI. Each method is exactly one HTTP call: it converts the response to our models and the
 * errors to {@link BallastException}. Combining calls is the bl's job.
 * <p>
 * Responses are cached (PokeAPI data is effectively static, and it respects the fair-use policy). The cache
 * works because the bl calls these methods through the Spring proxy of this bean. Below the cache, every real
 * HTTP call goes through {@link PokeApiResilience} (bulkhead, rate limiter, circuit breaker, retry), so a
 * cached response never consumes a permit.
 */
@Slf4j
@Service
public class PokeApiService implements PokemonCatalogPort {

    public static final String PAGES_CACHE = "pokeapi-pages";
    public static final String POKEMON_CACHE = "pokeapi-pokemon";
    public static final String SPECIES_CACHE = "pokeapi-species";
    public static final String EVOLUTION_CACHE = "pokeapi-evolution-chains";

    private final RestClient restClient;
    private final PokeApiMapper mapper;
    private final PokeApiResilience resilience;

    public PokeApiService(@Qualifier("pokeApiRestClient") RestClient restClient, PokeApiMapper mapper,
                          PokeApiResilience resilience) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.resilience = resilience;
    }

    @Override
    @Cacheable(cacheNames = PAGES_CACHE, key = "#offset + ':' + #limit")
    public NamePage fetchPage(long offset, int limit) {
        log.info("[SERVICE] Calling PokeAPI - GET /pokemon?offset={}&limit={}", offset, limit);
        NamePage page = mapper.toNamePage(get(PokeApiDto.Page.class, "/pokemon?offset={offset}&limit={limit}", offset, limit));
        log.info("[SERVICE] PokeAPI responded - total={}, names={}", page.total(), page.names());
        return page;
    }

    @Override
    @Cacheable(POKEMON_CACHE)
    public PokemonData fetchPokemon(String idOrName) {
        log.info("[SERVICE] Calling PokeAPI - GET /pokemon/{}", idOrName);
        PokemonData pokemon = mapper.toPokemonData(get(PokeApiDto.Pokemon.class, "/pokemon/{idOrName}", idOrName));
        log.info("[SERVICE] PokeAPI responded - id={}, name={}, species={}", pokemon.id(), pokemon.name(),
                pokemon.speciesName());
        return pokemon;
    }

    @Override
    @Cacheable(SPECIES_CACHE)
    public SpeciesData fetchSpecies(String speciesName) {
        log.info("[SERVICE] Calling PokeAPI - GET /pokemon-species/{}", speciesName);
        SpeciesData species = mapper.toSpeciesData(get(PokeApiDto.Species.class, "/pokemon-species/{name}", speciesName));
        log.info("[SERVICE] PokeAPI responded - species={}, category={}, evolutionChainId={}", speciesName,
                species.category(), species.evolutionChainId());
        return species;
    }

    @Override
    @Cacheable(EVOLUTION_CACHE)
    public EvolutionNode fetchEvolutionChain(int chainId) {
        log.info("[SERVICE] Calling PokeAPI - GET /evolution-chain/{}", chainId);
        EvolutionNode root = mapper.toEvolutionTree(get(PokeApiDto.EvolutionChain.class, "/evolution-chain/{id}", chainId));
        log.info("[SERVICE] PokeAPI responded - evolutionChainId={}, root={}", chainId, root.name());
        return root;
    }

    private <T> T get(Class<T> type, String uriTemplate, Object... uriVariables) {
        try {
            // the raw call runs inside the resilience decorators, which must see the original failures
            T body = resilience.execute(() -> restClient.get()
                    .uri(uriTemplate, uriVariables)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        log.info("[SERVICE] PokeAPI responded 404 - uri={}", request.getURI());
                        throw new BallastException(BallastErrorCode.POKEMON_NOT_FOUND);
                    })
                    .body(type));
            if (body == null) {
                throw new BallastException(BallastErrorCode.POKEAPI_UNAVAILABLE);
            }
            return body;
        } catch (RestClientException ex) {
            // 5xx, timeouts, connection errors, unreadable JSON, after the retries: not the caller's fault
            log.error("[SERVICE] PokeAPI call failed - uri={}", uriTemplate, ex);
            throw new BallastException(BallastErrorCode.POKEAPI_UNAVAILABLE);
        } catch (CallNotPermittedException ex) {
            log.warn("[SERVICE] PokeAPI circuit is open, failing fast - uri={}", uriTemplate);
            throw new BallastException(BallastErrorCode.POKEAPI_UNAVAILABLE);
        } catch (RequestNotPermitted | BulkheadFullException ex) {
            log.warn("[SERVICE] PokeAPI call limit reached - uri={}, limiter={}", uriTemplate, ex.getClass().getSimpleName());
            throw new BallastException(BallastErrorCode.POKEAPI_UNAVAILABLE);
        }
    }
}
