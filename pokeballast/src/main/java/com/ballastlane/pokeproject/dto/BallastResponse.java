package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Envelope of EVERY API response, success or error. */
@Schema(description = "Standard response envelope")
public record BallastResponse<T>(
        @Schema(description = "\"0000\" on success, otherwise an error code (see BallastErrorCode)", example = "0000",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        String responseCode,
        @Schema(description = "Business result on success; optional error data (e.g. field errors) on failure",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        T response,
        @Schema(description = "Error message; null on success",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        String errorDetail) {

    public static final String SUCCESS_CODE = "0000";

    public static <T> BallastResponse<T> success(T response) {
        return new BallastResponse<>(SUCCESS_CODE, response, null);
    }

    public static <T> BallastResponse<T> error(String code, T data, String errorDetail) {
        return new BallastResponse<>(code, data, errorDetail);
    }
}
