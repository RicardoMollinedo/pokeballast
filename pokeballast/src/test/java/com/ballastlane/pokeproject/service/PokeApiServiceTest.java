package com.ballastlane.pokeproject.service;

import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.NamePage;
import com.ballastlane.pokeproject.bl.model.PokemonData;
import com.ballastlane.pokeproject.bl.model.SpeciesData;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokeApiMapper;
import com.ballastlane.pokeproject.util.PokeApiUrlUtil;
import com.ballastlane.pokeproject.util.TextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Each method = one PokeAPI call, converted to our models, with errors translated to BallastException.
 * Runs against recorded payloads (src/test/resources/pokeapi), never the network.
 */
class PokeApiServiceTest {

    private static final String BASE = "https://pokeapi.test/api/v2";

    private MockRestServiceServer server;
    private PokeApiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        service = new PokeApiService(builder.build(), new PokeApiMapper(new PokeApiUrlUtil(), new TextNormalizer()),
                TestResilience.passThrough());
    }

    @Test
    void fetchPageReturnsNamesAndTotal() {
        expect("/pokemon?offset=0&limit=2", "pokemon-page.json");

        NamePage page = service.fetchPage(0, 2);

        assertThat(page.names()).containsExactly("bulbasaur", "ivysaur");
        assertThat(page.total()).isGreaterThan(1000);
    }

    @Test
    void fetchPokemonConvertsSpritesStatsTypesAbilitiesAndUnits() {
        expect("/pokemon/1", "pokemon-1.json");

        PokemonData pokemon = service.fetchPokemon("1");

        assertThat(pokemon.name()).isEqualTo("bulbasaur");
        assertThat(pokemon.speciesName()).isEqualTo("bulbasaur");
        assertThat(pokemon.spriteUrl()).endsWith("/sprites/pokemon/1.png");
        assertThat(pokemon.artworkUrl()).contains("official-artwork");
        assertThat(pokemon.types()).containsExactly("grass", "poison");
        assertThat(pokemon.abilities()).containsExactly("overgrow", "chlorophyll");
        assertThat(pokemon.measurements().weightKilograms()).isEqualTo(6.9);
        assertThat(pokemon.stats()).hasSize(6).first().satisfies(stat -> {
            assertThat(stat.name()).isEqualTo("hp");
            assertThat(stat.value()).isEqualTo(45);
        });
    }

    @Test
    void fetchSpeciesPicksTheEnglishGenusAndCleanDescription() {
        expect("/pokemon-species/bulbasaur", "species-1.json"); // Japanese genus and French text listed first

        SpeciesData species = service.fetchSpecies("bulbasaur");

        assertThat(species.category()).isEqualTo("Seed Pokémon");
        assertThat(species.description())
                .isEqualTo("A strange seed was planted on its back at birth. The plant sprouts and grows with this POKéMON.");
        assertThat(species.evolutionChainId()).isEqualTo(1);
    }

    @Test
    void fetchEvolutionChainBuildsTheTree() {
        expect("/evolution-chain/1", "evolution-chain-1.json");

        EvolutionNode root = service.fetchEvolutionChain(1);

        assertThat(root.name()).isEqualTo("bulbasaur");
        assertThat(root.evolvesTo()).singleElement().satisfies(ivysaur -> {
            assertThat(ivysaur.speciesId()).isEqualTo(2);
            assertThat(ivysaur.imageUrl()).endsWith("/sprites/pokemon/2.png");
            assertThat(ivysaur.evolvesTo()).singleElement()
                    .satisfies(venusaur -> assertThat(venusaur.name()).isEqualTo("venusaur"));
        });
    }

    @Test
    void upstream404BecomesPokemonNotFound() {
        server.expect(requestTo(BASE + "/pokemon/missingno")).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> service.fetchPokemon("missingno"))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEMON_NOT_FOUND.code()));
    }

    @Test
    void upstream5xxBecomesPokeApiUnavailable() {
        server.expect(requestTo(BASE + "/pokemon-species/bulbasaur")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.fetchSpecies("bulbasaur"))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEAPI_UNAVAILABLE.code());
                    assertThat(ex.getHttpStatus()).isEqualTo(503);
                });
    }

    private void expect(String path, String fixture) {
        server.expect(requestTo(BASE + path))
                .andRespond(withSuccess(new ClassPathResource("pokeapi/" + fixture), MediaType.APPLICATION_JSON));
    }
}
