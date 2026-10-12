package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Result of a successful login")
public record AuthResponse(
        @Schema(description = "Signed JWT; send it as 'Authorization: Bearer <token>'",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        String accessToken,
        @Schema(description = "Always 'Bearer'", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false)
        String tokenType,
        @Schema(description = "Instant the token expires (UTC)", requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = false)
        Instant expiresAt,
        @Schema(description = "The authenticated user", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        UserResponse user) {
}
