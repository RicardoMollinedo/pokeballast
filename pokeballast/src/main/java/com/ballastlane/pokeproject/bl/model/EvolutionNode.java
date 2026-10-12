package com.ballastlane.pokeproject.bl.model;

import java.util.List;

/** Evolution lineage is a tree, not a list: Eevee branches into eight evolutions. */
public record EvolutionNode(int speciesId, String name, String imageUrl, List<EvolutionNode> evolvesTo) {

    public EvolutionNode {
        evolvesTo = List.copyOf(evolvesTo);
    }
}
