package com.ballastlane.pokeproject.bl.model;

import java.util.List;

/** One page of the PokeAPI index: only names, plus the total number of pokemon. */
public record NamePage(List<String> names, long total) {

    public NamePage {
        names = List.copyOf(names);
    }
}
