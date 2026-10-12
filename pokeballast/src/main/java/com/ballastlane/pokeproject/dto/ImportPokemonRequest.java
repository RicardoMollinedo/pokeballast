package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pokemon to replicate from PokeAPI (US03)")
public record ImportPokemonRequest(
        @Schema(description = "PokeAPI id or name: a positive number or a slug like 'mr-mime'", example = "pikachu",
                maxLength = 50, requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank String idOrName) {
}
