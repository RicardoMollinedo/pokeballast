package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.LocalPokemonBl;
import com.ballastlane.pokeproject.bl.PokemonSyncBl;
import com.ballastlane.pokeproject.dto.ImportPokemonRequest;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.UpdateLocalPokemonRequest;
import com.ballastlane.pokeproject.config.SecurityConfig;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import com.ballastlane.pokeproject.util.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static com.ballastlane.pokeproject.support.TestData.storedPikachu;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocalPokemonController.class)
@Import({SecurityConfig.class, LogSanitizer.class})
class LocalPokemonControllerTest {

    private static final RequestPostProcessor TRAINER = jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINER"));
    private static final RequestPostProcessor ADMIN = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final RequestPostProcessor NO_ROLE = jwt().authorities(List.of());

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private LocalPokemonBl localPokemonBl;
    @MockitoBean
    private PokemonSyncBl pokemonSyncBl;

    // ---------- read: public ----------

    @Test
    void listIsPublicAndPassesFilters() throws Exception {
        when(localPokemonBl.list("pika", "mascot", 0, 20))
                .thenReturn(new PageResponse<>(List.of(response(1L, 3)), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/local-pokemon").param("name", "pika").param("tag", "mascot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.content[0].localizedName").value("Pikachu ES"))
                .andExpect(jsonPath("$.response.content[0].region").value("Kanto"))
                .andExpect(jsonPath("$.response.content[0].tags[0]").value("mascot"))
                .andExpect(jsonPath("$.response.content[0].version").value(3));
    }

    @Test
    void getUnknownIdIs404() throws Exception {
        when(localPokemonBl.get(99L)).thenThrow(new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND));

        mvc.perform(get("/api/v1/local-pokemon/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("2002"));
    }

    @Test
    void nonNumericIdIsAMalformedRequest() throws Exception {
        mvc.perform(get("/api/v1/local-pokemon/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"));
    }

    // ---------- US03: import / refresh ----------

    @Test
    void importWithoutTokenIs401InTheStandardEnvelope() throws Exception {
        mvc.perform(post("/api/v1/local-pokemon").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrName\":\"pikachu\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4001"));
        verifyNoInteractions(pokemonSyncBl);
    }

    @Test
    void importReturns201WithLocation() throws Exception {
        when(pokemonSyncBl.importPokemon(new ImportPokemonRequest("pikachu"))).thenReturn(response(5L, 0));

        mvc.perform(post("/api/v1/local-pokemon").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrName\":\"pikachu\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/local-pokemon/5"))
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.id").value(5));
    }

    @Test
    void importWithBlankIdentifierIs400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/local-pokemon").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrName\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1000"))
                .andExpect(jsonPath("$.response.idOrName").exists());
        verifyNoInteractions(pokemonSyncBl);
    }

    @Test
    void importingTwiceIs409() throws Exception {
        when(pokemonSyncBl.importPokemon(new ImportPokemonRequest("25"))).thenThrow(new BallastException(BallastErrorCode.POKEMON_ALREADY_IMPORTED));

        mvc.perform(post("/api/v1/local-pokemon").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrName\":\"25\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value("3001"));
    }

    @Test
    void userWithoutAnAllowedRoleCannotImport() throws Exception {
        mvc.perform(post("/api/v1/local-pokemon").with(NO_ROLE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrName\":\"25\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("4003"));
    }

    @Test
    void refreshRequiresATokenAndReturnsTheRecord() throws Exception {
        when(pokemonSyncBl.refresh(1L)).thenReturn(response(1L, 2));

        mvc.perform(post("/api/v1/local-pokemon/1/refresh")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/local-pokemon/1/refresh").with(TRAINER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.version").value(2));
    }

    // ---------- US04: update ----------

    @Test
    void updateMapsTheBodyToTheBlAndReturnsTheNewVersion() throws Exception {
        UpdateLocalPokemonRequest expected = new UpdateLocalPokemonRequest("Pika", "Kanto", List.of("mascot"), 3L);
        when(localPokemonBl.update(1L, expected)).thenReturn(response(1L, 4));

        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"localizedName":"Pika","region":"Kanto","tags":["mascot"],"version":3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.version").value(4));
    }

    @Test
    void updateWithoutVersionIs400() throws Exception {
        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localizedName\":\"Pika\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1000"))
                .andExpect(jsonPath("$.response.version").exists());
        verifyNoInteractions(localPokemonBl);
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\": [unclosed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"));
    }

    @Test
    void unknownFieldIs400() throws Exception {
        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"hp\":999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"));
        verifyNoInteractions(localPokemonBl);
    }

    @Test
    void wrongValueTypeIs400() throws Exception {
        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":\"latest\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"));
    }

    @Test
    void businessValidationErrorsAre400WithFieldDetails() throws Exception {
        when(localPokemonBl.update(anyLong(), any())).thenThrow(new BallastException(BallastErrorCode.INVALID_CUSTOMIZATION,
                "Invalid proprietary fields", Map.of("tags", "at most 10 tags are allowed")));

        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"a\"],\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1004"))
                .andExpect(jsonPath("$.response.tags").value("at most 10 tags are allowed"));
    }

    @Test
    void updateOfMissingRecordIs404() throws Exception {
        when(localPokemonBl.update(anyLong(), any())).thenThrow(new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND));

        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("2002"));
    }

    @Test
    void staleVersionIs409() throws Exception {
        when(localPokemonBl.update(anyLong(), any())).thenThrow(new BallastException(BallastErrorCode.STALE_VERSION));

        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value("3002"));
    }

    @Test
    void updateWithoutTokenIs401() throws Exception {
        mvc.perform(put("/api/v1/local-pokemon/1").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4001"));
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mvc.perform(patch("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.responseCode").value("1005"));
    }

    @Test
    void nonJsonBodyIs415() throws Exception {
        mvc.perform(put("/api/v1/local-pokemon/1").with(TRAINER).contentType(MediaType.TEXT_PLAIN).content("version=0"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.responseCode").value("1006"));
    }

    // ---------- delete: admin only ----------

    @Test
    void trainersCannotDelete() throws Exception {
        mvc.perform(delete("/api/v1/local-pokemon/1").with(TRAINER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.responseCode").value("4003"));
        verifyNoInteractions(localPokemonBl);
    }

    @Test
    void adminCanDeleteAndGets204WithoutBody() throws Exception {
        mvc.perform(delete("/api/v1/local-pokemon/1").with(ADMIN))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(localPokemonBl).delete(1L);
    }

    @Test
    void deletingAMissingRecordIs404() throws Exception {
        doThrow(new BallastException(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND)).when(localPokemonBl).delete(1L);

        mvc.perform(delete("/api/v1/local-pokemon/1").with(ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseCode").value("2002"));
    }

    private static LocalPokemonResponse response(long id, long version) {
        return new PokemonResponseMapper().toLocal(storedPikachu(id, version));
    }
}
