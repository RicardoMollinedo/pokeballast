package com.ballastlane.pokeproject.dto.pokeapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wire format of the PokeAPI endpoints we consume. Only the fields we need are declared and
 * unknown ones are ignored, so upstream additions never break deserialization.
 * These types never leave {@code service} / {@code mapper}.
 */
public final class PokeApiDto {

    private PokeApiDto() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedResource(String name, String url) {
    }

    /** {@code GET /pokemon?offset&limit} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(long count, List<NamedResource> results) {
    }

    /** {@code GET /pokemon/{idOrName}}; height in decimetres, weight in hectograms. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pokemon(int id, String name, int height, int weight, List<AbilitySlot> abilities,
                          List<StatEntry> stats, List<TypeSlot> types, NamedResource species, Sprites sprites) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AbilitySlot(@JsonProperty("is_hidden") boolean hidden, int slot, NamedResource ability) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatEntry(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TypeSlot(int slot, NamedResource type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artwork(@JsonProperty("front_default") String frontDefault) {
    }

    /** {@code GET /pokemon-species/{idOrName}} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Species(int id, String name, @JsonProperty("evolution_chain") ApiResource evolutionChain,
                          List<Genus> genera, @JsonProperty("flavor_text_entries") List<FlavorText> flavorTextEntries) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiResource(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genus(String genus, NamedResource language) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlavorText(@JsonProperty("flavor_text") String text, NamedResource language) {
    }

    /** {@code GET /evolution-chain/{id}} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvolutionChain(int id, ChainLink chain) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainLink(NamedResource species, @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {
    }
}
