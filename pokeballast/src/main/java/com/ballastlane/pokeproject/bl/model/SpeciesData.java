package com.ballastlane.pokeproject.bl.model;

/**
 * Data of the PokeAPI {@code /pokemon-species} resource, already in our terms.
 *
 * @param category         English genus, e.g. "Seed Pokémon"; null if PokeAPI has none
 * @param description      first English flavor text, cleaned; null if none
 * @param evolutionChainId null if PokeAPI links no chain
 */
public record SpeciesData(String category, String description, Integer evolutionChainId) {
}
