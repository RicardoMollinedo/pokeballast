package com.ballastlane.pokeproject.support;

import com.ballastlane.pokeproject.bl.model.BaseStat;
import com.ballastlane.pokeproject.bl.model.BodyMeasurements;
import com.ballastlane.pokeproject.bl.model.Customization;
import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.PokemonData;
import com.ballastlane.pokeproject.bl.model.PokemonDetail;
import com.ballastlane.pokeproject.bl.model.SpeciesData;

import java.time.Instant;
import java.util.List;

/** Shared fixtures, so each test only spells out what it actually cares about. */
public final class TestData {

    public static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    public static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");

    private TestData() {
    }

    public static PokemonDetail pikachuDetail() {
        return pikachuDetail(60);
    }

    public static PokemonDetail pikachuDetail(int weightHg) {
        EvolutionNode raichu = new EvolutionNode(26, "raichu", "raichu.png", List.of());
        EvolutionNode pikachu = new EvolutionNode(25, "pikachu", "pikachu.png", List.of(raichu));
        EvolutionNode pichu = new EvolutionNode(172, "pichu", "pichu.png", List.of(pikachu));
        return new PokemonDetail(25, "pikachu", "artwork/25.png", "sprite/25.png", "Mouse Pokémon",
                "It keeps its tail raised to monitor its surroundings.", List.of("electric"),
                List.of("static", "lightning-rod"), new BodyMeasurements(4, weightHg),
                List.of(new BaseStat("hp", 35), new BaseStat("speed", 90)), pichu);
    }

    public static PokemonDetail simpleDetail(int id, String name) {
        return new PokemonDetail(id, name, null, null, null, null, List.of("psychic"), List.of("synchronize"),
                new BodyMeasurements(4, 40), List.of(), new EvolutionNode(id, name, null, List.of()));
    }

    public static PokemonData pokemonData(int id, String name) {
        return new PokemonData(id, name, name, "sprite/" + id + ".png", "artwork/" + id + ".png", List.of("electric"),
                List.of("static", "lightning-rod"), new BodyMeasurements(4, 60),
                List.of(new BaseStat("hp", 35), new BaseStat("speed", 90)));
    }

    public static SpeciesData speciesData(Integer chainId) {
        return new SpeciesData("Mouse Pokémon", "It keeps its tail raised.", chainId);
    }

    /** What the repository returns after an insert: the same record with its generated id. */
    public static LocalPokemon withId(LocalPokemon pokemon, long id) {
        return new LocalPokemon(id, pokemon.pokeApiId(), pokemon.name(), pokemon.spriteUrl(), pokemon.category(),
                pokemon.types(), pokemon.abilities(), pokemon.measurements(), pokemon.customization(), pokemon.version(),
                pokemon.syncedAt(), pokemon.updatedAt());
    }

    public static LocalPokemon storedPikachu(long id, long version) {
        return new LocalPokemon(id, 25, "pikachu", "sprite/25.png", "Mouse Pokémon", List.of("electric"),
                List.of("static"), new BodyMeasurements(4, 60),
                new Customization("Pikachu ES", "Kanto", List.of("mascot")), version, T0, T0);
    }
}
