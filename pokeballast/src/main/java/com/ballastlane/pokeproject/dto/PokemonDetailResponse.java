package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pokemon detail (US02)")
public record PokemonDetailResponse(
        @Schema(description = "PokeAPI id", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
        int id,
        @Schema(description = "PokeAPI name (slug)", example = "pikachu",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        String name,
        @Schema(description = "Official artwork URL (falls back to the sprite); null if PokeAPI has none",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String imageUrl,
        @Schema(description = "Front sprite URL; null if PokeAPI has none",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String spriteUrl,
        @Schema(description = "Pokedex category (English genus)", example = "Mouse Pokémon",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String category,
        @Schema(description = "Narrative description (first English flavor text)",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String description,
        @Schema(description = "Types in slot order", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> types,
        @Schema(description = "Abilities in slot order", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> abilities,
        @Schema(description = "Height in meters", example = "0.4", requiredMode = Schema.RequiredMode.REQUIRED)
        double heightM,
        @Schema(description = "Mass in kilograms", example = "6.0", requiredMode = Schema.RequiredMode.REQUIRED)
        double weightKg,
        @Schema(description = "Core statistics (base values)", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<StatResponse> stats,
        @Schema(description = "Evolution lineage as a tree, rooted at the first stage; null if PokeAPI has none",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        EvolutionResponse evolutionChain) {

    @Schema(description = "Base statistic")
    public record StatResponse(
            @Schema(description = "Stat name", example = "speed", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
            String name,
            @Schema(description = "Base value (0-255)", example = "90", requiredMode = Schema.RequiredMode.REQUIRED)
            int value) {
    }

    @Schema(description = "Node of the evolution tree")
    public record EvolutionResponse(
            @Schema(description = "Species id", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
            int id,
            @Schema(description = "Species name", example = "pikachu", requiredMode = Schema.RequiredMode.REQUIRED,
                    nullable = false)
            String name,
            @Schema(description = "Sprite URL", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
            String imageUrl,
            @Schema(description = "Next stages; several for branching lineages (e.g. Eevee), empty for the last stage",
                    requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
            List<EvolutionResponse> evolvesTo) {
    }
}
