package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.port.LocalPokemonPort;
import com.ballastlane.pokeproject.bl.port.PokemonCatalogPort;
import com.ballastlane.pokeproject.dto.ImportPokemonRequest;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ballastlane.pokeproject.support.TestData.T1;
import static com.ballastlane.pokeproject.support.TestData.pokemonData;
import static com.ballastlane.pokeproject.support.TestData.speciesData;
import static com.ballastlane.pokeproject.support.TestData.storedPikachu;
import static com.ballastlane.pokeproject.support.TestData.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonSyncBlTest {

    @Mock
    private PokemonCatalogPort catalog;
    @Mock
    private LocalPokemonPort localPokemon;
    private ExecutorService executor;
    private PokemonSyncBl bl;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        PokemonResponseMapper mapper = new PokemonResponseMapper();
        bl = new PokemonSyncBl(new PokemonCatalogBl(catalog, mapper, executor), localPokemon, mapper,
                Clock.fixed(T1, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    private void pokeApiKnowsPikachu(String identifier) {
        when(catalog.fetchPokemon(identifier)).thenReturn(pokemonData(25, "pikachu"));
        when(catalog.fetchSpecies("pikachu")).thenReturn(speciesData(null));
    }

    @Test
    void importsAPokemonThatIsNotStoredYet() {
        pokeApiKnowsPikachu("pikachu");
        when(localPokemon.existsByPokeApiId(25)).thenReturn(false);
        when(localPokemon.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), 5L));

        LocalPokemonResponse imported = bl.importPokemon(new ImportPokemonRequest("Pikachu"));

        assertThat(imported.id()).isEqualTo(5L);
        assertThat(imported.pokeApiId()).isEqualTo(25);
        assertThat(imported.category()).isEqualTo("Mouse Pokémon");
        assertThat(imported.syncedAt()).isEqualTo(T1);
    }

    @Test
    void importingTheSamePokemonTwiceIsAConflict() {
        pokeApiKnowsPikachu("25");
        when(localPokemon.existsByPokeApiId(25)).thenReturn(true);

        assertThatThrownBy(() -> bl.importPokemon(new ImportPokemonRequest("25")))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.POKEMON_ALREADY_IMPORTED.code());
                    assertThat(ex.getHttpStatus()).isEqualTo(409);
                });
        verify(localPokemon, never()).save(any());
    }

    @Test
    void invalidIdentifierNeverReachesPokeApi() {
        assertThatThrownBy(() -> bl.importPokemon(new ImportPokemonRequest("pika chu"))).isInstanceOf(BallastException.class);
        verifyNoInteractions(catalog, localPokemon);
    }

    @Test
    void refreshKeepsProprietaryFieldsAndVersion() {
        LocalPokemon stored = storedPikachu(7L, 2);
        when(localPokemon.findById(7L)).thenReturn(Optional.of(stored));
        pokeApiKnowsPikachu("25");
        when(localPokemon.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalPokemonResponse refreshed = bl.refresh(7L);

        ArgumentCaptor<LocalPokemon> saved = ArgumentCaptor.forClass(LocalPokemon.class);
        verify(localPokemon).save(saved.capture());
        assertThat(saved.getValue().customization()).isEqualTo(stored.customization());
        assertThat(saved.getValue().version()).isEqualTo(2);
        assertThat(refreshed.localizedName()).isEqualTo("Pikachu ES");
        assertThat(refreshed.syncedAt()).isEqualTo(T1);
    }

    @Test
    void refreshOfAnUnknownLocalIdIsNotFound() {
        when(localPokemon.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bl.refresh(99L))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND.code()));
        verifyNoInteractions(catalog);
    }
}
