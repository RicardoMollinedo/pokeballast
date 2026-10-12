package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public data of a user (never the password hash)")
public record UserResponse(
        @Schema(description = "Username (lower case)", example = "ash", requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false)
        String username,
        @Schema(description = "TRAINER or ADMIN", example = "TRAINER", requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false)
        String role) {
}
