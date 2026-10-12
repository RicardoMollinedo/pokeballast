package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Schema(description = "Full replacement of a task (omitted optional fields are cleared)")
public record UpdateTaskRequest(
        @Schema(description = "1-120 characters", requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank String title,
        @Schema(description = "Up to 2000 characters", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String description,
        @Schema(description = "TODO, IN_PROGRESS or DONE; must follow the workflow", example = "IN_PROGRESS",
                allowableValues = {"TODO", "IN_PROGRESS", "DONE"}, requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank String status,
        @Schema(description = "ISO date; a new date cannot be in the past", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        LocalDate dueDate,
        @Schema(description = "Version read by the client (optimistic locking)", example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotNull @PositiveOrZero Long version) {
}
