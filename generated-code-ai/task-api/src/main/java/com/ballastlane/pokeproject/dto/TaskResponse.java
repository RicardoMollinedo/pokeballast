package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "A task of the authenticated user (the owner is implicit and never exposed)")
public record TaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String title,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) String description,
        @Schema(example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) String status,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true) LocalDate dueDate,
        @Schema(description = "Send it back when updating", requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = false) Instant updatedAt) {
}
