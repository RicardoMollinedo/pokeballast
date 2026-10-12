package com.ballastlane.pokeproject.bl.model;

import java.util.List;

/** One entry of the browse list (US01): sprite, category, mass and skills (abilities). */
public record PokemonSummary(
        int id,
        String name,
        String spriteUrl,
        String category,
        List<String> types,
        List<String> abilities,
        BodyMeasurements measurements) {

    public PokemonSummary {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
    }

    /** Combines the two PokeAPI resources a list entry needs. */
    public static PokemonSummary of(PokemonData pokemon, SpeciesData species) {
        return new PokemonSummary(pokemon.id(), pokemon.name(), pokemon.spriteUrl(), species.category(),
                pokemon.types(), pokemon.abilities(), pokemon.measurements());
    }
}
