package com.ballastlane.pokeproject.bl.model;

/** Optional filters of the local collection; {@code null} means "do not filter". */
public record LocalPokemonFilter(String name, String tag) {

    public static LocalPokemonFilter none() {
        return new LocalPokemonFilter(null, null);
    }
}
