package com.ballastlane.pokeproject.bl.port;

import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.LocalPokemonFilter;
import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;

import java.util.Optional;

/** Persistence of the local replica. Implemented in {@code repository.adapter}. */
public interface LocalPokemonPort {

    Optional<LocalPokemon> findById(long id);

    boolean existsById(long id);

    boolean existsByPokeApiId(int pokeApiId);

    PageResult<LocalPokemon> search(LocalPokemonFilter filter, PageQuery pageQuery);

    /**
     * Inserts ({@code id == null}) or updates the pokemon.
     *
     * @throws com.ballastlane.pokeproject.exceptions.BallastException STALE_VERSION (3002) if the stored
     *                                                                 version differs from the given one
     */
    LocalPokemon save(LocalPokemon pokemon);

    void deleteById(long id);
}
