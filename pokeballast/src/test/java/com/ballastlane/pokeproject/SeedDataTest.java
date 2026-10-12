package com.ballastlane.pokeproject;

import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The demo seed (database/scripts/seed) applies cleanly and gives a usable app right after startup. */
@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
        "spring.datasource.url=jdbc:h2:mem:seed-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"})
@AutoConfigureMockMvc
class SeedDataTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PokemonCatalogPort catalog; // the seed must not need PokeAPI

    @Test
    void demoUsersCanLogInWithTheDocumentedPasswords() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ash\",\"password\":\"Pikachu123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.user.role").value("TRAINER"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.user.role").value("ADMIN"));
    }

    @Test
    void demoPokemonAreStoredWithProprietaryFields() throws Exception {
        mvc.perform(get("/api/v1/local-pokemon").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.totalElements").value(8))
                .andExpect(jsonPath("$.response.content[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$.response.content[0].localizedName").value("フシギダネ"));
        mvc.perform(get("/api/v1/local-pokemon").param("tag", "starter"))
                .andExpect(jsonPath("$.response.totalElements").value(3));
    }
}
