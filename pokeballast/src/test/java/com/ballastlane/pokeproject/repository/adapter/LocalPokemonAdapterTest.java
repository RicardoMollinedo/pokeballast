package com.ballastlane.pokeproject.repository.adapter;

import com.ballastlane.pokeproject.bl.model.Customization;
import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.LocalPokemonFilter;
import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.LocalPokemonEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static com.ballastlane.pokeproject.support.TestData.T0;
import static com.ballastlane.pokeproject.support.TestData.T1;
import static com.ballastlane.pokeproject.support.TestData.pikachuDetail;
import static com.ballastlane.pokeproject.support.TestData.simpleDetail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real Flyway migrations and JPQL queries against H2 in PostgreSQL mode.
 * Each test runs in a transaction that is rolled back afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// own in-memory database: data committed by @SpringBootTest flows must not leak into these assertions
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@Import({LocalPokemonAdapter.class, LocalPokemonEntityMapper.class})
class LocalPokemonAdapterTest {

    @Autowired
    private LocalPokemonAdapter adapter;

    private LocalPokemon pikachu;
    private LocalPokemon bulbasaur;
    private LocalPokemon mew;

    @BeforeEach
    void seed() {
        pikachu = adapter.save(LocalPokemon.importFrom(pikachuDetail(), T0)
                .customize(new Customization("ピカチュウ", "Kanto", List.of("mascot", "electric-team")), T0));
        bulbasaur = adapter.save(LocalPokemon.importFrom(simpleDetail(1, "bulbasaur"), T0)
                .customize(new Customization(null, "Kanto", List.of("starter")), T0));
        mew = adapter.save(LocalPokemon.importFrom(simpleDetail(151, "mew"), T0));
    }

    @Test
    void savesAndReadsBackEveryField() {
        LocalPokemon found = adapter.findById(pikachu.id()).orElseThrow();

        assertThat(found.pokeApiId()).isEqualTo(25);
        assertThat(found.category()).isEqualTo("Mouse Pokémon");
        assertThat(found.types()).containsExactly("electric");
        assertThat(found.abilities()).containsExactly("static", "lightning-rod");
        assertThat(found.measurements().weightHectograms()).isEqualTo(60);
        assertThat(found.customization().tags()).as("tag order is kept").containsExactly("mascot", "electric-team");
        assertThat(found.version()).isZero();
    }

    @Test
    void listsOrderedByPokeApiIdWithPaging() {
        PageResult<LocalPokemon> firstPage = adapter.search(LocalPokemonFilter.none(), new PageQuery(0, 2));

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.items()).extracting(LocalPokemon::name).containsExactly("bulbasaur", "pikachu");
        assertThat(adapter.search(LocalPokemonFilter.none(), new PageQuery(1, 2)).items())
                .extracting(LocalPokemon::name).containsExactly("mew");
    }

    @Test
    void filtersByNameOrLocalizedNameIgnoringCase() {
        assertThat(adapter.search(new LocalPokemonFilter("CHU", null), new PageQuery(0, 10)).items())
                .extracting(LocalPokemon::name).containsExactly("pikachu");
        assertThat(adapter.search(new LocalPokemonFilter("ピカ", null), new PageQuery(0, 10)).items())
                .extracting(LocalPokemon::name).containsExactly("pikachu");
    }

    @Test
    void filtersByTag() {
        assertThat(adapter.search(new LocalPokemonFilter(null, "STARTER"), new PageQuery(0, 10)).items())
                .extracting(LocalPokemon::name).containsExactly("bulbasaur");
    }

    @Test
    void combinesNameAndTagFilters() {
        assertThat(adapter.search(new LocalPokemonFilter("bulba", "mascot"), new PageQuery(0, 10)).items()).isEmpty();
    }

    @Test
    void likeWildcardsInTheSearchTermAreLiteral() {
        assertThat(adapter.search(new LocalPokemonFilter("%", null), new PageQuery(0, 10)).items()).isEmpty();
        assertThat(adapter.search(new LocalPokemonFilter("_", null), new PageQuery(0, 10)).items()).isEmpty();
    }

    @Test
    void updateIncrementsTheVersionAndRejectsStaleCopies() {
        LocalPokemon updated = adapter.save(mew.customize(new Customization("ミュウ", null, List.of("mythical")), T1));

        assertThat(updated.version()).isEqualTo(mew.version() + 1);
        assertThat(updated.customization().tags()).containsExactly("mythical");
        assertThatThrownBy(() -> adapter.save(mew.customize(Customization.empty(), T1)))
                .as("the old copy still has version 0")
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.STALE_VERSION.code()));
    }

    @Test
    void existenceChecks() {
        assertThat(adapter.existsByPokeApiId(25)).isTrue();
        assertThat(adapter.existsByPokeApiId(4)).isFalse();
        assertThat(adapter.existsById(bulbasaur.id())).isTrue();
        assertThat(adapter.existsById(-1)).isFalse();
    }

    @Test
    void deleteRemovesThePokemonAndItsTags() {
        adapter.deleteById(pikachu.id());

        assertThat(adapter.findById(pikachu.id())).isEmpty();
        assertThat(adapter.search(new LocalPokemonFilter(null, "mascot"), new PageQuery(0, 10)).items()).isEmpty();
    }

    @Test
    void duplicatedPokeApiIdIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> adapter.save(LocalPokemon.importFrom(pikachuDetail(), T1)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.CONCURRENT_MODIFICATION.code()));
    }
}
