package com.ballastlane.pokeproject.bl.port;

import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.NamePage;
import com.ballastlane.pokeproject.bl.model.PokemonData;
import com.ballastlane.pokeproject.bl.model.SpeciesData;

/**
 * External PokeAPI: one method per PokeAPI resource, each a single call whose output is converted to our
 * models and whose errors become {@code BallastException} (2001 not found, 5001 unavailable). Combining
 * several calls (US01 enrichment, US02 detail) is business logic and lives in {@code bl}.
 */
public interface PokemonCatalogPort {

    /** {@code GET /pokemon?offset&limit}: names only. */
    NamePage fetchPage(long offset, int limit);

    /** {@code GET /pokemon/{idOrName}}. */
    PokemonData fetchPokemon(String idOrName);

    /** {@code GET /pokemon-species/{name}}. */
    SpeciesData fetchSpecies(String speciesName);

    /** {@code GET /evolution-chain/{id}}, as a tree. */
    EvolutionNode fetchEvolutionChain(int chainId);
}
