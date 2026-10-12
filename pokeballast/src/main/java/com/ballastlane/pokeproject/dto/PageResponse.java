package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Single paging structure used by every paginated endpoint. */
@Schema(description = "One page of results")
public record PageResponse<T>(
        @Schema(description = "Items of this page (empty when the page is past the end)",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        List<T> content,
        @Schema(description = "Zero-based page number", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "Requested page size", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "Total number of items", example = "1351", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
        @Schema(description = "Total number of pages", example = "68", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages) {
}
