package com.ballastlane.pokeproject.bl.model;

import java.util.List;

/**
 * Data of the PokeAPI {@code /pokemon} resource, already in our terms.
 *
 * @param speciesName needed to fetch the species (it differs from the name for alternate forms, e.g. deoxys-normal)
 */
public record PokemonData(
        int id,
        String name,
        String speciesName,
        String spriteUrl,
        String artworkUrl,
        List<String> types,
        List<String> abilities,
        BodyMeasurements measurements,
        List<BaseStat> stats) {

    public PokemonData {
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
        stats = List.copyOf(stats);
    }
}
