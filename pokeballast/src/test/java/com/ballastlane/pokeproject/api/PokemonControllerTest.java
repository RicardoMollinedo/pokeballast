package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.PokemonCatalogBl;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.PokemonSummaryResponse;
import com.ballastlane.pokeproject.config.SecurityConfig;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import com.ballastlane.pokeproject.util.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.ballastlane.pokeproject.support.TestData.pikachuDetail;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PokemonController.class)
@Import({SecurityConfig.class, LogSanitizer.class})
class PokemonControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PokemonCatalogBl pokemonCatalogBl;

    // ---------- US01 ----------

    @Test
    void browseIsPublicAndWrapsThePageInABallastResponse() throws Exception {
        PokemonSummaryResponse bulbasaur = new PokemonSummaryResponse(1, "bulbasaur", "1.png", "Seed Pokémon",
                List.of("grass", "poison"), List.of("overgrow", "chlorophyll"), 0.7, 6.9);
        when(pokemonCatalogBl.browse(2, 1)).thenReturn(new PageResponse<>(List.of(bulbasaur), 2, 1, 1351, 1351));

        mvc.perform(get("/api/v1/pokemon").param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.errorDetail").value(nullValue()))
                .andExpect(jsonPath("$.response.content[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.response.content[0].spriteUrl").value("1.png"))
                .andExpect(jsonPath("$.response.content[0].category").value("Seed Pokémon"))
                .andExpect(jsonPath("$.response.content[0].weightKg").value(6.9))
                .andExpect(jsonPath("$.response.content[0].abilities[1]").value("chlorophyll"))
                .andExpect(jsonPath("$.response.totalElements").value(1351))
                .andExpect(jsonPath("$.response.totalPages").value(1351));
    }

    @Test
    void browseUsesDefaultPaging() throws Exception {
        when(pokemonCatalogBl.browse(0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/pokemon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.size").value(20));
    }

    @Test
    void invalidPagingIs400WithFieldErrors() throws Exception {
        when(pokemonCatalogBl.browse(anyInt(), anyInt())).thenThrow(new BallastException(
                BallastErrorCode.INVALID_PAGINATION, "Invalid pagination parameters", Map.of("size", "must be between 1 and 50")));

        mvc.perform(get("/api/v1/pokemon").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1002"))
                .andExpect(jsonPath("$.response.size").value("must be between 1 and 50"))
                .andExpect(jsonPath("$.errorDetail").value("Invalid pagination parameters"));
    }

    @Test
    void nonNumericPagingIsAMalformedRequest() throws Exception {
        mvc.perform(get("/api/v1/pokemon").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"));
    }

    @Test
    void auditHeadersAreOptionalAndAccepted() throws Exception {
        when(pokemonCatalogBl.browse(0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/pokemon")
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                        .header("x-user", "ash")
                        .header("x-latitude", "not-a-number")
                        .header("x-longitude", "-70.6")
                        .header("x-channel", "WEB"))
                .andExpect(status().isOk());
    }

    // ---------- US02 ----------

    @Test
    void detailReturnsImageStatsDescriptionAndEvolutionTree() throws Exception {
        when(pokemonCatalogBl.getDetail("pikachu")).thenReturn(new PokemonResponseMapper().toDetail(pikachuDetail()));

        mvc.perform(get("/api/v1/pokemon/pikachu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.imageUrl").value("artwork/25.png"))
                .andExpect(jsonPath("$.response.stats[1].name").value("speed"))
                .andExpect(jsonPath("$.response.stats[1].value").value(90))
                .andExpect(jsonPath("$.response.description").isNotEmpty())
                .andExpect(jsonPath("$.response.evolutionChain.name").value("pichu"))
                .andExpect(jsonPath("$.response.evolutionChain.evolvesTo[0].evolvesTo[0].name").value("raichu"));
    }

    @Test
    void unknownPokemonIs404() throws Exception {
        when(pokemonCatalogBl.getDetail("missingno")).thenThrow(new BallastException(BallastErrorCode.POKEMON_NOT_FOUND));

        mvc.perform(get("/api/v1/pokemon/missingno"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("2001"))
                .andExpect(jsonPath("$.errorDetail").value("Pokemon not found"));
    }

    @Test
    void pokeApiOutageIs503() throws Exception {
        when(pokemonCatalogBl.getDetail(anyString())).thenThrow(new BallastException(BallastErrorCode.POKEAPI_UNAVAILABLE));

        mvc.perform(get("/api/v1/pokemon/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.responseCode").value("5001"));
    }

    @Test
    void unexpectedErrorsAre500WithoutInternals() throws Exception {
        when(pokemonCatalogBl.getDetail(anyString())).thenThrow(new IllegalStateException("db password is hunter2"));

        mvc.perform(get("/api/v1/pokemon/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.responseCode").value("9999"))
                .andExpect(jsonPath("$.errorDetail").value("Unexpected error"));
    }

    @Test
    void unknownRouteIs404InTheStandardEnvelopeForAuthenticatedCallers() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("2000"));
    }

    @Test
    void unknownRouteIs401ForAnonymousCallersSoRoutesAreNotDisclosed() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4001"));
    }
}
