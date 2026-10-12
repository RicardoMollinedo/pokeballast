package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.LocalPokemonBl;
import com.ballastlane.pokeproject.bl.PokemonSyncBl;
import com.ballastlane.pokeproject.dto.BallastResponse;
import com.ballastlane.pokeproject.dto.ImportPokemonRequest;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.UpdateLocalPokemonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** CRUD over the local replica (US03 and US04). */
@Slf4j
@RestController
@RequestMapping("/api/v1/local-pokemon")
@RequiredArgsConstructor
@Tag(name = "Local pokemon", description = "Pokemon replicated into our database, with proprietary fields")
public class LocalPokemonController {

    private final LocalPokemonBl localPokemonBl;
    private final PokemonSyncBl pokemonSyncBl;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "List the local collection, optionally filtered by name and tag",
            description = """
                    Paginated list of the pokemon stored in our database, ordered by PokeAPI id.

                    **User story:** US03 - Data synchronization (reading the replica).

                    **Validations:**
                    - `page` >= 0 and 1 <= `size` <= 50 (400, code 1002).
                    - `name` matches the name or the localized name, case-insensitive; `%` and `_` are literal.
                    - `tag` is compared case-insensitively.

                    **Role:** public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "responseCode 0000; response = page of local pokemon"),
            @ApiResponse(responseCode = "400", description = "Invalid paging (1001 / 1002)")})
    public ResponseEntity<BallastResponse<PageResponse<LocalPokemonResponse>>> list(
            @Parameter(description = "Text contained in the name or localized name (optional)", example = "pika")
            @RequestParam(required = false) String name,
            @Parameter(description = "Exact tag (optional)", example = "starter")
            @RequestParam(required = false) String tag,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1 to 50", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        log.info("[REQUEST] List local pokemon - name={}, tag={}, page={}, size={}", name, tag, page, size);

        PageResponse<LocalPokemonResponse> body = localPokemonBl.list(name, tag, page, size);

        log.info("[REQUEST] List local pokemon responded - totalElements={}, ids={}", body.totalElements(),
                body.content().stream().map(LocalPokemonResponse::id).toList());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Get one local pokemon",
            description = """
                    Returns a pokemon stored in our database, including its proprietary fields and its `version` \
                    (needed to update it).

                    **User story:** US03 - Data synchronization (reading the replica).

                    **Validations:**
                    - `id` must be numeric (400, code 1001).
                    - `id` must exist (404, code 2002).

                    **Role:** public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "responseCode 0000; response = the local pokemon"),
            @ApiResponse(responseCode = "400", description = "Non-numeric id (1001)"),
            @ApiResponse(responseCode = "404", description = "Local pokemon not found (2002)")})
    public ResponseEntity<BallastResponse<LocalPokemonResponse>> get(
            @Parameter(description = "Local id", required = true, example = "4") @PathVariable long id) {
        log.info("[REQUEST] Get local pokemon - id={}", id);

        LocalPokemonResponse body = localPokemonBl.get(id);

        log.info("[REQUEST] Get local pokemon responded - id={}, name={}, version={}", body.id(), body.name(), body.version());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(
            summary = "Import (replicate) a pokemon from PokeAPI into the local database",
            security = @SecurityRequirement(name = "bearer"),
            description = """
                    Fetches the pokemon from PokeAPI and stores it locally with empty proprietary fields, ready to \
                    be enriched.

                    **User story:** US03 - Data synchronization.

                    **Validations:**
                    - `idOrName` is required (400, code 1000) and must be a positive number or a slug (400, code 1003).
                    - Unknown pokemon (404, code 2001).
                    - Already stored (409, code 3001); a concurrent import of the same pokemon is also rejected \
                    by the database (409, code 3003).
                    - PokeAPI down (503, code 5001).

                    **Role:** TRAINER or ADMIN.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created; Location header; response = the stored pokemon"),
            @ApiResponse(responseCode = "400", description = "Invalid body or identifier (1000 / 1001 / 1003)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token (4001)"),
            @ApiResponse(responseCode = "403", description = "Role not allowed (4003)"),
            @ApiResponse(responseCode = "404", description = "Pokemon not found in PokeAPI (2001)"),
            @ApiResponse(responseCode = "409", description = "Already imported (3001 / 3003)"),
            @ApiResponse(responseCode = "503", description = "PokeAPI unavailable (5001)")})
    public ResponseEntity<BallastResponse<LocalPokemonResponse>> importPokemon(
            @Valid @RequestBody ImportPokemonRequest request) {
        log.info("[REQUEST] Import pokemon - idOrName={}", request.idOrName());

        LocalPokemonResponse body = pokemonSyncBl.importPokemon(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(body.id()).toUri();

        log.info("[REQUEST] Import pokemon responded - id={}, pokeApiId={}, name={}", body.id(), body.pokeApiId(), body.name());
        return ResponseEntity.created(location).body(BallastResponse.success(body));
    }

    @PostMapping("/{id}/refresh")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(
            summary = "Re-sync a local pokemon with PokeAPI, keeping its proprietary fields",
            security = @SecurityRequirement(name = "bearer"),
            description = """
                    Pulls fresh upstream data (name, sprite, category, types, abilities, measurements) into an \
                    existing record. localizedName, region and tags are kept.

                    **User story:** US03 - Data synchronization.

                    **Validations:**
                    - `id` must exist (404, code 2002).
                    - Modified concurrently (409, code 3002 / 3003).
                    - PokeAPI down (503, code 5001).

                    **Role:** TRAINER or ADMIN.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "responseCode 0000; response = the refreshed pokemon"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token (4001)"),
            @ApiResponse(responseCode = "403", description = "Role not allowed (4003)"),
            @ApiResponse(responseCode = "404", description = "Local pokemon not found (2002)"),
            @ApiResponse(responseCode = "409", description = "Concurrent modification (3002 / 3003)"),
            @ApiResponse(responseCode = "503", description = "PokeAPI unavailable (5001)")})
    public ResponseEntity<BallastResponse<LocalPokemonResponse>> refresh(
            @Parameter(description = "Local id", required = true, example = "4") @PathVariable long id) {
        log.info("[REQUEST] Refresh local pokemon - id={}", id);

        LocalPokemonResponse body = pokemonSyncBl.refresh(id);

        log.info("[REQUEST] Refresh local pokemon responded - id={}, version={}, syncedAt={}",
                body.id(), body.version(), body.syncedAt());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(
            summary = "Update the proprietary fields of a local pokemon",
            security = @SecurityRequirement(name = "bearer"),
            description = """
                    Replaces localizedName, region and tags (full replacement: a null or blank field clears it). \
                    Replicated PokeAPI data is read-only (use refresh).

                    **User story:** US04 - Local data modification.

                    **Validations:**
                    - `id` must exist (404, code 2002).
                    - `version` is required (400, code 1000) and must match the stored version (409, code 3002).
                    - localizedName/region at most 60 chars; at most 10 tags of 1-30 lowercase letters, digits and \
                    single hyphens (400, code 1004, with one message per field). Tags are lower-cased and de-duplicated.
                    - Invalid JSON, wrong value types or unknown fields (400, code 1001).

                    **Role:** TRAINER or ADMIN.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "responseCode 0000; response = the updated pokemon (new version)"),
            @ApiResponse(responseCode = "400", description = "Invalid or malformed payload (1000 / 1001 / 1004)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token (4001)"),
            @ApiResponse(responseCode = "403", description = "Role not allowed (4003)"),
            @ApiResponse(responseCode = "404", description = "Local pokemon not found (2002)"),
            @ApiResponse(responseCode = "409", description = "Stale version or concurrent modification (3002 / 3003)")})
    public ResponseEntity<BallastResponse<LocalPokemonResponse>> update(
            @Parameter(description = "Local id", required = true, example = "4") @PathVariable long id,
            @Valid @RequestBody UpdateLocalPokemonRequest request) {
        log.info("[REQUEST] Update local pokemon - id={}, localizedName={}, region={}, tags={}, version={}",
                id, request.localizedName(), request.region(), request.tags(), request.version());

        LocalPokemonResponse body = localPokemonBl.update(id, request);

        log.info("[REQUEST] Update local pokemon responded - id={}, version={}", body.id(), body.version());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a local pokemon",
            security = @SecurityRequirement(name = "bearer"),
            description = """
                    Removes a pokemon (and its tags) from our database. Success is **204 No Content without a \
                    body**: the only deliberate exception to the BallastResponse envelope. Errors keep the envelope.

                    **User story:** US04 - Local data modification.

                    **Validations:**
                    - `id` must exist (404, code 2002).

                    **Role:** ADMIN.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted; no body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token (4001)"),
            @ApiResponse(responseCode = "403", description = "Not an admin (4003)"),
            @ApiResponse(responseCode = "404", description = "Local pokemon not found (2002)")})
    public ResponseEntity<Void> delete(
            @Parameter(description = "Local id", required = true, example = "4") @PathVariable long id) {
        log.info("[REQUEST] Delete local pokemon - id={}", id);

        localPokemonBl.delete(id);

        log.info("[REQUEST] Delete local pokemon responded - id={}, status=204", id);
        return ResponseEntity.noContent().build();
    }
}
