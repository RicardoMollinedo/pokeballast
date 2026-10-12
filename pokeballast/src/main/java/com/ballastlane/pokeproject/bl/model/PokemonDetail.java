package com.ballastlane.pokeproject.bl.model;

import java.util.List;

/** Full view of one pokemon (US02): image, stats, description and evolution lineage. */
public record PokemonDetail(
        int id,
        String name,
        String imageUrl,
        String spriteUrl,
        String category,
        String description,
        List<String> types,
        List<String> abilities,
        BodyMeasurements measurements,
        List<BaseStat> stats,
        EvolutionNode evolutionChain) {

    public PokemonDetail {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
        stats = List.copyOf(stats);
    }

    /**
     * Combines the three PokeAPI resources of the detail view.
     *
     * @param evolutionChain null when PokeAPI links no chain
     */
    public static PokemonDetail of(PokemonData pokemon, SpeciesData species, EvolutionNode evolutionChain) {
        String image = pokemon.artworkUrl() != null ? pokemon.artworkUrl() : pokemon.spriteUrl();
        return new PokemonDetail(pokemon.id(), pokemon.name(), image, pokemon.spriteUrl(), species.category(),
                species.description(), pokemon.types(), pokemon.abilities(), pokemon.measurements(), pokemon.stats(),
                evolutionChain);
    }
}
