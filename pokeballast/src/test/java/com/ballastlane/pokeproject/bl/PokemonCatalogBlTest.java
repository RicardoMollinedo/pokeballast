package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.EvolutionNode;
import com.ballastlane.pokeproject.bl.model.NamePage;
import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.PokemonDetailResponse;
import com.ballastlane.pokeproject.dto.PokemonSummaryResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ballastlane.pokeproject.support.TestData.pokemonData;
import static com.ballastlane.pokeproject.support.TestData.speciesData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonCatalogBlTest {

    @Mock
    private PokemonCatalogPort catalog;
    private ExecutorService executor;
    private PokemonCatalogBl bl;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor(); // the real fan-out, as in production
        bl = new PokemonCatalogBl(catalog, new PokemonResponseMapper(), executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    // ---------- US01 ----------

    @Test
    void browseEnrichesEveryNameWithPokemonAndSpeciesData() {
        when(catalog.fetchPage(40, 2)).thenReturn(new NamePage(List.of("pikachu", "raichu"), 1351));
        when(catalog.fetchPokemon("pikachu")).thenReturn(pokemonData(25, "pikachu"));
        when(catalog.fetchPokemon("raichu")).thenReturn(pokemonData(26, "raichu"));
        when(catalog.fetchSpecies("pikachu")).thenReturn(speciesData(10));
        when(catalog.fetchSpecies("raichu")).thenReturn(speciesData(10));

        PageResponse<PokemonSummaryResponse> page = bl.browse(20, 2);

        assertThat(page.content()).extracting(PokemonSummaryResponse::name).containsExactly("pikachu", "raichu");
        PokemonSummaryResponse first = page.content().getFirst();
        assertThat(first.category()).isEqualTo("Mouse Pokémon");
        assertThat(first.spriteUrl()).isEqualTo("sprite/25.png");
        assertThat(first.weightKg()).isEqualTo(6.0);
        assertThat(first.abilities()).containsExactly("static", "lightning-rod");
        assertThat(page.totalElements()).isEqualTo(1351);
        assertThat(page.totalPages()).isEqualTo(676);
        assertThat(page.page()).isEqualTo(20);
    }

    @Test
    void browseRejectsInvalidPagingBeforeCallingPokeApi() {
        assertThatThrownBy(() -> bl.browse(-1, 500))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_PAGINATION.code()));
        verifyNoInteractions(catalog);
    }

    @Test
    void aFailureInsideTheFanOutSurfacesAsTheOriginalBallastException() {
        when(catalog.fetchPage(0, 1)).thenReturn(new NamePage(List.of("pikachu"), 1351));
        when(catalog.fetchPokemon("pikachu")).thenThrow(new BallastException(BallastErrorCode.POKEAPI_UNAVAILABLE));

        assertThatThrownBy(() -> bl.browse(0, 1))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEAPI_UNAVAILABLE.code()));
    }

    // ---------- US02 ----------

    @Test
    void detailCombinesPokemonSpeciesAndEvolutionChain() {
        EvolutionNode chain = new EvolutionNode(172, "pichu", "pichu.png",
                List.of(new EvolutionNode(25, "pikachu", "pikachu.png", List.of())));
        when(catalog.fetchPokemon("pikachu")).thenReturn(pokemonData(25, "pikachu"));
        when(catalog.fetchSpecies("pikachu")).thenReturn(speciesData(10));
        when(catalog.fetchEvolutionChain(10)).thenReturn(chain);

        PokemonDetailResponse detail = bl.getDetail(" Pikachu ");

        assertThat(detail.imageUrl()).as("official artwork first").isEqualTo("artwork/25.png");
        assertThat(detail.description()).isEqualTo("It keeps its tail raised.");
        assertThat(detail.stats()).hasSize(2);
        assertThat(detail.evolutionChain().name()).isEqualTo("pichu");
        assertThat(detail.evolutionChain().evolvesTo()).singleElement()
                .satisfies(next -> assertThat(next.name()).isEqualTo("pikachu"));
    }

    @Test
    void detailWithoutEvolutionChainDoesNotCallTheChainEndpoint() {
        when(catalog.fetchPokemon("pikachu")).thenReturn(pokemonData(25, "pikachu"));
        when(catalog.fetchSpecies("pikachu")).thenReturn(speciesData(null));

        assertThat(bl.getDetail("pikachu").evolutionChain()).isNull();
        verify(catalog, never()).fetchEvolutionChain(anyInt());
    }

    @Test
    void detailRejectsInvalidIdentifiersBeforeCallingPokeApi() {
        assertThatThrownBy(() -> bl.getDetail("../etc/passwd")).isInstanceOf(BallastException.class);
        verifyNoInteractions(catalog);
    }

    @Test
    void detailPropagatesNotFound() {
        when(catalog.fetchPokemon("missingno")).thenThrow(new BallastException(BallastErrorCode.POKEMON_NOT_FOUND));

        assertThatThrownBy(() -> bl.getDetail("missingno"))
                .isInstanceOfSatisfying(BallastException.class, ex -> assertThat(ex.getHttpStatus()).isEqualTo(404));
    }
}
