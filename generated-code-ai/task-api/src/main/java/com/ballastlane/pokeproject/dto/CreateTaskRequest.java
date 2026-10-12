package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** Shape only; the business rules (lengths, due date) live in bl.model.TaskContent. */
@Schema(description = "New task (it starts as TODO)")
public record CreateTaskRequest(
        @Schema(description = "1-120 characters", example = "Catch a Pikachu", maxLength = 120,
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank String title,
        @Schema(description = "Up to 2000 characters; blank becomes null", example = "Viridian Forest, early morning",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String description,
        @Schema(description = "ISO date, not in the past", example = "2026-12-31",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        LocalDate dueDate) {
}
