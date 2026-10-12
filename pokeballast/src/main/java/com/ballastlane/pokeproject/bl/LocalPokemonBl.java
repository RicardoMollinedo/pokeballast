package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.Customization;
import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.LocalPokemonFilter;
import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.port.LocalPokemonPort;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.UpdateLocalPokemonRequest;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Read, update and delete of the local replica (US03 reads, US04). */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalPokemonBl {

    private final LocalPokemonPort localPokemon;
    private final PokemonResponseMapper mapper;
    private final Clock clock;

    /**
     * Lists the local collection.
     *
     * <ol>
     *   <li>Validates the paging; otherwise throws {@code INVALID_PAGINATION} (1002, 400).</li>
     *   <li>Searches through {@link LocalPokemonPort#search}, filtering by name/localized name and by tag
     *       when they are given.</li>
     *   <li>Maps the page to the response DTO.</li>
     * </ol>
     */
    public PageResponse<LocalPokemonResponse> list(String name, String tag, int page, int size) {
        PageQuery pageQuery = new PageQuery(page, size);
        log.info("[BL] Listing local pokemon - name={}, tag={}, page={}, size={}", name, tag, page, size);
        return mapper.toPage(localPokemon.search(new LocalPokemonFilter(name, tag), pageQuery), mapper::toLocal);
    }

    /**
     * Returns one local pokemon.
     *
     * <ol>
     *   <li>Loads it through {@link LocalPokemonPort#findById}; if absent, throws
     *       {@code LOCAL_POKEMON_NOT_FOUND} (2002, 404).</li>
     *   <li>Maps it to the response DTO.</li>
     * </ol>
     */
    public LocalPokemonResponse get(long id) {
        return mapper.toLocal(load(id));
    }

    /**
     * Replaces the proprietary fields of a stored pokemon (US04).
     *
     * <ol>
     *   <li>Loads the pokemon; if absent, throws {@code LOCAL_POKEMON_NOT_FOUND} (2002, 404).</li>
     *   <li>Compares the stored version with {@code request.version()}; if they differ, throws
     *       {@code STALE_VERSION} (3002, 409) to prevent lost updates.</li>
     *   <li>Builds the new {@link Customization}: trims texts (blank becomes null), lower-cases and de-duplicates
     *       tags, enforces max 60 chars per text and max 10 well-formed tags; otherwise throws
     *       {@code INVALID_CUSTOMIZATION} (1004, 400) with one message per field.</li>
     *   <li>Saves the change with the current timestamp ({@link LocalPokemonPort#save} also guards the version
     *       at DB level).</li>
     *   <li>Returns the updated pokemon (with its new version) as the response DTO.</li>
     * </ol>
     *
     * @throws BallastException 2002, 3002 or 1004 as described above
     */
    public LocalPokemonResponse update(long id, UpdateLocalPokemonRequest request) {
        LocalPokemon current = load(id);
        if (current.version() != request.version()) {
            log.info("[BL] Rejecting update with stale version - id={}, expected={}, actual={}",
                    id, request.version(), current.version());
            throw new BallastException(BallastErrorCode.STALE_VERSION);
        }
        Customization customization = new Customization(request.localizedName(), request.region(), request.tags());
        log.info("[BL] Applying customization - id={}", id);
        return mapper.toLocal(localPokemon.save(current.customize(customization, clock.instant())));
    }

    /**
     * Deletes a local pokemon.
     *
     * <ol>
     *   <li>Checks it exists; otherwise throws {@code LOCAL_POKEMON_NOT_FOUND} (2002, 404).</li>
     *   <li>Deletes it (its tags are removed by the DB cascade).</li>
     * </ol>
     */
    public void delete(long id) {
        if (!localPokemon.existsById(id)) {
            throw new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND);
        }
        log.info("[BL] Deleting local pokemon - id={}", id);
        localPokemon.deleteById(id);
    }

    private LocalPokemon load(long id) {
        return localPokemon.findById(id)
                .orElseThrow(() -> new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND));
    }
}
