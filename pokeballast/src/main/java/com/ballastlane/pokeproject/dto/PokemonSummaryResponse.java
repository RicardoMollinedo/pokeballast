package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pokemon list entry (US01)")
public record PokemonSummaryResponse(
        @Schema(description = "PokeAPI id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int id,
        @Schema(description = "PokeAPI name (slug)", example = "bulbasaur",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        String name,
        @Schema(description = "Front sprite URL; null if PokeAPI has none",
                example = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String spriteUrl,
        @Schema(description = "Pokedex category (English genus); null if PokeAPI has none", example = "Seed Pokémon",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String category,
        @Schema(description = "Types in slot order", example = "[\"grass\",\"poison\"]",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> types,
        @Schema(description = "Skills: abilities in slot order", example = "[\"overgrow\",\"chlorophyll\"]",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> abilities,
        @Schema(description = "Height in meters", example = "0.7", requiredMode = Schema.RequiredMode.REQUIRED)
        double heightM,
        @Schema(description = "Mass in kilograms", example = "6.9", requiredMode = Schema.RequiredMode.REQUIRED)
        double weightKg) {
}
