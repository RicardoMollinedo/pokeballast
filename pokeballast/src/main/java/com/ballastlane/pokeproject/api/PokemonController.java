package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.PokemonCatalogBl;
import com.ballastlane.pokeproject.dto.BallastResponse;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.PokemonDetailResponse;
import com.ballastlane.pokeproject.dto.PokemonSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only view over PokeAPI (US01 and US02). */
@Slf4j
@RestController
@RequestMapping("/api/v1/pokemon")
@RequiredArgsConstructor
@Tag(name = "PokeAPI catalog", description = "Browse and inspect pokemon straight from PokeAPI (cached)")
public class PokemonController {

    private final PokemonCatalogBl pokemonCatalogBl;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Paginated list of pokemon with sprite, category, mass and skills",
            description = """
                    Returns one page of pokemon from PokeAPI. Each entry has its sprite, category (Pokedex genus), \
                    mass in kg and its skills (abilities).

                    **User story:** US01 - Pokemon list.

                    **Validations:**
                    - `page` >= 0 and 1 <= `size` <= 50 (400, code 1002, with one message per field).
                    - Non-numeric `page`/`size` (400, code 1001).
                    - PokeAPI down or timing out (503, code 5001).

                    Responses are cached, so repeated pages do not call PokeAPI again.

                    **Role:** public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "responseCode 0000; response = page of pokemon"),
            @ApiResponse(responseCode = "400", description = "Invalid paging (1001 / 1002)"),
            @ApiResponse(responseCode = "503", description = "PokeAPI unavailable (5001)")})
    public ResponseEntity<BallastResponse<PageResponse<PokemonSummaryResponse>>> browse(
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1 to 50", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        log.info("[REQUEST] Browse pokemon - page={}, size={}", page, size);

        PageResponse<PokemonSummaryResponse> body = pokemonCatalogBl.browse(page, size);

        log.info("[REQUEST] Browse pokemon responded - totalElements={}, names={}", body.totalElements(),
                body.content().stream().map(PokemonSummaryResponse::name).toList());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @GetMapping("/{idOrName}")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Pokemon detail: image, stats, description and evolution lineage",
            description = """
                    Returns the comprehensive data of one pokemon: official artwork, base statistics, narrative \
                    description (first English flavor text) and the evolution lineage as a tree (it branches, e.g. Eevee).

                    **User story:** US02 - Pokemon detail.

                    **Validations:**
                    - `idOrName` must be a positive number or a slug like `mr-mime` (400, code 1003); it is never \
                    forwarded to PokeAPI otherwise.
                    - Unknown pokemon (404, code 2001).
                    - PokeAPI down or timing out (503, code 5001).

                    **Role:** public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "responseCode 0000; response = pokemon detail"),
            @ApiResponse(responseCode = "400", description = "Invalid identifier (1003)"),
            @ApiResponse(responseCode = "404", description = "Pokemon not found (2001)"),
            @ApiResponse(responseCode = "503", description = "PokeAPI unavailable (5001)")})
    public ResponseEntity<BallastResponse<PokemonDetailResponse>> detail(
            @Parameter(description = "PokeAPI id or name", required = true, example = "pikachu")
            @PathVariable String idOrName) {
        log.info("[REQUEST] Get pokemon detail - idOrName={}", idOrName);

        PokemonDetailResponse body = pokemonCatalogBl.getDetail(idOrName);

        log.info("[REQUEST] Get pokemon detail responded - id={}, name={}, stats={}", body.id(), body.name(),
                body.stats().size());
        return ResponseEntity.ok(BallastResponse.success(body));
    }
}
