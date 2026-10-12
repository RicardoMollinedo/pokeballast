package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.NamePage;
import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.model.PokemonData;
import com.ballastlane.pokeproject.bl.model.PokemonDetail;
import com.ballastlane.pokeproject.bl.model.PokemonIdentifier;
import com.ballastlane.pokeproject.bl.model.PokemonSummary;
import com.ballastlane.pokeproject.bl.model.SpeciesData;
import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.PokemonDetailResponse;
import com.ballastlane.pokeproject.dto.PokemonSummaryResponse;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/** Read-only view over the PokeAPI catalog (US01 and US02): combines the PokeAPI resources. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PokemonCatalogBl {

    private final PokemonCatalogPort catalog;
    private final PokemonResponseMapper mapper;
    /** Virtual threads: the enrichment calls are pure I/O waiting. */
    private final ExecutorService pokeApiExecutor;

    /**
     * Returns one page of pokemon with sprite, category, mass and abilities (US01).
     *
     * <ol>
     *   <li>Validates the paging: {@code page >= 0} and {@code 1 <= size <= 50}; otherwise throws
     *       {@code INVALID_PAGINATION} (1002, 400) without calling PokeAPI.</li>
     *   <li>Fetches the page of names through {@link PokemonCatalogPort#fetchPage} (PokeAPI only returns names).</li>
     *   <li>For every name, concurrently: {@link PokemonCatalogPort#fetchPokemon} (sprite, weight, abilities) and then
     *       {@link PokemonCatalogPort#fetchSpecies} (category). Every call is cached by the service.</li>
     *   <li>Combines each pair into a summary, keeping the PokeAPI order. If any call fails, its
     *       {@code BallastException} (2001 / 5001) is rethrown as is.</li>
     *   <li>Maps the page to the response DTO.</li>
     * </ol>
     */
    public PageResponse<PokemonSummaryResponse> browse(int page, int size) {
        PageQuery pageQuery = new PageQuery(page, size);
        log.info("[BL] Browsing pokemon catalog - page={}, size={}", page, size);
        NamePage names = catalog.fetchPage(pageQuery.offset(), pageQuery.size());
        List<CompletableFuture<PokemonSummary>> futures = names.names().stream()
                .map(name -> CompletableFuture.supplyAsync(() -> loadSummary(name), pokeApiExecutor))
                .toList();
        List<PokemonSummary> summaries = futures.stream().map(PokemonCatalogBl::join).toList();
        log.info("[BL] Catalog page enriched - total={}, items={}", names.total(), summaries.size());
        return mapper.toPage(new PageResult<>(summaries, page, size, names.total()), mapper::toSummary);
    }

    /**
     * Returns the full detail of one pokemon: image, stats, description and evolution tree (US02).
     *
     * <ol>
     *   <li>Builds the detail with {@link #loadDetail} (validation + the three PokeAPI calls).</li>
     *   <li>Maps it to the response DTO.</li>
     * </ol>
     *
     * @throws com.ballastlane.pokeproject.exceptions.BallastException 1003, 2001 or 5001
     */
    public PokemonDetailResponse getDetail(String idOrName) {
        return mapper.toDetail(loadDetail(PokemonIdentifier.of(idOrName)));
    }

    /**
     * Combines the PokeAPI resources of one pokemon. Also used by {@link PokemonSyncBl} to import it.
     *
     * <ol>
     *   <li>{@link PokemonCatalogPort#fetchPokemon} with the validated identifier (2001 if unknown).</li>
     *   <li>{@link PokemonCatalogPort#fetchSpecies} with the species name (it differs from the pokemon name for
     *       alternate forms, e.g. deoxys-normal): category and description.</li>
     *   <li>{@link PokemonCatalogPort#fetchEvolutionChain} only if the species links a chain.</li>
     *   <li>Combines the three into a {@link PokemonDetail}.</li>
     * </ol>
     */
    public PokemonDetail loadDetail(PokemonIdentifier identifier) {
        log.info("[BL] Loading pokemon detail - identifier={}", identifier);
        PokemonData pokemon = catalog.fetchPokemon(identifier.value());
        SpeciesData species = catalog.fetchSpecies(pokemon.speciesName());
        EvolutionNode chain = species.evolutionChainId() == null ? null
                : catalog.fetchEvolutionChain(species.evolutionChainId());
        return PokemonDetail.of(pokemon, species, chain);
    }

    private PokemonSummary loadSummary(String name) {
        PokemonData pokemon = catalog.fetchPokemon(name);
        return PokemonSummary.of(pokemon, catalog.fetchSpecies(pokemon.speciesName()));
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            // surface the original BallastException instead of the async wrapper
            if (ex.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw ex;
        }
    }
}
