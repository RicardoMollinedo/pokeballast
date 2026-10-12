package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Full replacement (PUT) of the proprietary fields. Only the request shape is validated here;
 * the business rules (lengths, tag format, max tags) live in {@code bl.model.Customization}.
 */
@Schema(description = "Proprietary fields of a local pokemon (full replacement, US04)")
public record UpdateLocalPokemonRequest(
        @Schema(description = "Localized name; null or blank clears it", example = "ピカチュウ", maxLength = 60,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String localizedName,
        @Schema(description = "Geographical metadata (region); null or blank clears it", example = "Kanto", maxLength = 60,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String region,
        @Schema(description = "Internal tags: max 10, each 1-30 chars of lowercase letters, digits and single hyphens. "
                + "Normalized to lower case and de-duplicated; null clears them",
                example = "[\"mascot\",\"electric-team\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        List<String> tags,
        @Schema(description = "Version returned by the last read (optimistic locking)", example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotNull @PositiveOrZero Long version) {
}
