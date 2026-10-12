package com.ballastlane.pokeproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Body of register and login. Password strength rules are enforced in bl ({@code UserCredentials}). */
@Schema(description = "Username and password")
public record CredentialsRequest(
        @Schema(description = "3-30 letters, digits or underscore; case-insensitive", example = "ash",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false)
        @NotBlank String username,
        @Schema(description = "8-72 characters with at least one letter and one digit", example = "Pikachu123",
                requiredMode = Schema.RequiredMode.REQUIRED, nullable = false, format = "password")
        @NotBlank String password) {

    @Override
    public String toString() {
        return "CredentialsRequest[username=" + username + ", password=***]"; // never log the password
    }
}
