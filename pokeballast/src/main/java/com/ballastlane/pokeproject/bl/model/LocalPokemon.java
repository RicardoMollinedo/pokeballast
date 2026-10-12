package com.ballastlane.pokeproject.bl.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A pokemon replicated into our database. Immutable: every change returns a new instance.
 *
 * @param id      local primary key, {@code null} until persisted
 * @param version optimistic-locking version, used to reject lost updates
 */
public record LocalPokemon(
        Long id,
        int pokeApiId,
        String name,
        String spriteUrl,
        String category,
        List<String> types,
        List<String> abilities,
        BodyMeasurements measurements,
        Customization customization,
        long version,
        Instant syncedAt,
        Instant updatedAt) {

    public LocalPokemon {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(measurements, "measurements");
        types = List.copyOf(types);
        abilities = List.copyOf(abilities);
        customization = customization == null ? Customization.empty() : customization;
    }

    /** First replication of a PokeAPI entry. */
    public static LocalPokemon importFrom(PokemonDetail source, Instant now) {
        return new LocalPokemon(null, source.id(), source.name(), source.spriteUrl(), source.category(),
                source.types(), source.abilities(), source.measurements(), Customization.empty(), 0, now, now);
    }

    /** Re-sync of upstream data that keeps every proprietary field. */
    public LocalPokemon refreshFrom(PokemonDetail source, Instant now) {
        if (source.id() != pokeApiId) {
            throw new IllegalArgumentException(
                    "Cannot refresh pokemon %d with data of pokemon %d".formatted(pokeApiId, source.id()));
        }
        return new LocalPokemon(id, pokeApiId, source.name(), source.spriteUrl(), source.category(),
                source.types(), source.abilities(), source.measurements(), customization, version, now, now);
    }

    public LocalPokemon customize(Customization newCustomization, Instant now) {
        return new LocalPokemon(id, pokeApiId, name, spriteUrl, category, types, abilities, measurements,
                newCustomization, version, syncedAt, now);
    }
}
