package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Pokemon stored in our database (US03/US04)")
public record LocalPokemonResponse(
        @Schema(description = "Local id (primary key)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        long id,
        @Schema(description = "PokeAPI id", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
        int pokeApiId,
        @Schema(description = "PokeAPI name (replicated)", example = "pikachu",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        String name,
        @Schema(description = "Sprite URL (replicated)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String spriteUrl,
        @Schema(description = "Category (replicated)", example = "Mouse Pokémon",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String category,
        @Schema(description = "Types (replicated)", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> types,
        @Schema(description = "Abilities (replicated)", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> abilities,
        @Schema(description = "Height in meters (replicated)", example = "0.4", requiredMode = Schema.RequiredMode.REQUIRED)
        double heightM,
        @Schema(description = "Mass in kilograms (replicated)", example = "6.0", requiredMode = Schema.RequiredMode.REQUIRED)
        double weightKg,
        @Schema(description = "Proprietary: localized name", example = "ピカチュウ",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String localizedName,
        @Schema(description = "Proprietary: geographical metadata", example = "Kanto",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String region,
        @Schema(description = "Proprietary: internal classification tags (may be empty)", example = "[\"mascot\"]",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<String> tags,
        @Schema(description = "Optimistic-locking version; send it back when updating", example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long version,
        @Schema(description = "Last sync with PokeAPI", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        Instant syncedAt,
        @Schema(description = "Last change", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        Instant updatedAt) {
}
