package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.PokemonDetail;
import com.ballastlane.pokeproject.bl.model.PokemonIdentifier;
import com.ballastlane.pokeproject.bl.port.LocalPokemonPort;
import com.ballastlane.pokeproject.dto.ImportPokemonRequest;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Replication of PokeAPI data into the local relational store (US03). */
@Slf4j
@Service
@RequiredArgsConstructor
public class PokemonSyncBl {

    private final PokemonCatalogBl pokemonCatalogBl;
    private final LocalPokemonPort localPokemon;
    private final PokemonResponseMapper mapper;
    private final Clock clock;

    /**
     * Imports a pokemon from PokeAPI into the local database.
     *
     * <ol>
     *   <li>Validates the identifier; otherwise throws {@code INVALID_POKEMON_IDENTIFIER} (1003, 400).</li>
     *   <li>Loads the PokeAPI data through {@link PokemonCatalogBl#loadDetail} (2001 if unknown, 5001 if PokeAPI is down).</li>
     *   <li>Checks {@link LocalPokemonPort#existsByPokeApiId}; if it is already stored, throws
     *       {@code POKEMON_ALREADY_IMPORTED} (3001, 409). The DB unique constraint covers concurrent imports.</li>
     *   <li>Builds the local record with empty proprietary fields and the current timestamp, and saves it.</li>
     *   <li>Returns the stored pokemon (with its generated id) as the response DTO.</li>
     * </ol>
     *
     * @throws BallastException 1003, 2001, 3001 or 5001 as described above
     */
    public LocalPokemonResponse importPokemon(ImportPokemonRequest request) {
        PokemonIdentifier identifier = PokemonIdentifier.of(request.idOrName());
        PokemonDetail source = pokemonCatalogBl.loadDetail(identifier);
        if (localPokemon.existsByPokeApiId(source.id())) {
            log.info("[BL] Pokemon already imported, rejecting - pokeApiId={}", source.id());
            throw new BallastException(BallastErrorCode.POKEMON_ALREADY_IMPORTED);
        }
        log.info("[BL] Importing pokemon - pokeApiId={}, name={}", source.id(), source.name());
        return mapper.toLocal(localPokemon.save(LocalPokemon.importFrom(source, clock.instant())));
    }

    /**
     * Re-syncs the upstream data of a stored pokemon, keeping its proprietary fields.
     *
     * <ol>
     *   <li>Loads the local record; if absent, throws {@code LOCAL_POKEMON_NOT_FOUND} (2002, 404).</li>
     *   <li>Fetches fresh data from PokeAPI by the stored PokeAPI id (5001 if PokeAPI is down).</li>
     *   <li>Replaces the replicated fields (name, sprite, category, types, abilities, measurements), keeps
     *       localizedName/region/tags and the version, and updates the sync timestamp.</li>
     *   <li>Saves it and returns the refreshed pokemon as the response DTO.</li>
     * </ol>
     *
     * @throws BallastException 2002, 5001 (or 3002 if it was modified concurrently)
     */
    public LocalPokemonResponse refresh(long localId) {
        LocalPokemon current = localPokemon.findById(localId)
                .orElseThrow(() -> new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND));
        PokemonDetail source = pokemonCatalogBl.loadDetail(PokemonIdentifier.of(String.valueOf(current.pokeApiId())));
        log.info("[BL] Refreshing local pokemon from PokeAPI - id={}, pokeApiId={}", localId, current.pokeApiId());
        return mapper.toLocal(localPokemon.save(current.refreshFrom(source, clock.instant())));
    }
}
