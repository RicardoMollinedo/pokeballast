package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.LocalPokemon;
import com.ballastlane.pokeproject.bl.model.LocalPokemonFilter;
import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.port.LocalPokemonPort;
import com.ballastlane.pokeproject.dto.LocalPokemonResponse;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.UpdateLocalPokemonRequest;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.PokemonResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ballastlane.pokeproject.support.TestData.T1;
import static com.ballastlane.pokeproject.support.TestData.storedPikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalPokemonBlTest {

    @Mock
    private LocalPokemonPort localPokemon;
    private LocalPokemonBl bl;

    @BeforeEach
    void setUp() {
        bl = new LocalPokemonBl(localPokemon, new PokemonResponseMapper(), Clock.fixed(T1, ZoneOffset.UTC));
    }

    // ---------- read ----------

    @Test
    void listPassesFiltersAndPaging() {
        PageResult<LocalPokemon> page = new PageResult<>(List.of(storedPikachu(1L, 0)), 0, 20, 1);
        when(localPokemon.search(new LocalPokemonFilter("pika", "mascot"), new PageQuery(0, 20))).thenReturn(page);

        PageResponse<LocalPokemonResponse> result = bl.list("pika", "mascot", 0, 20);

        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.localizedName()).isEqualTo("Pikachu ES");
            assertThat(item.tags()).containsExactly("mascot");
        });
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listRejectsInvalidPaging() {
        assertThatThrownBy(() -> bl.list(null, null, 0, 0)).isInstanceOf(BallastException.class);
    }

    @Test
    void getReturnsTheStoredPokemon() {
        when(localPokemon.findById(1L)).thenReturn(Optional.of(storedPikachu(1L, 0)));

        assertThat(bl.get(1L).name()).isEqualTo("pikachu");
    }

    @Test
    void getUnknownIdIsNotFound() {
        when(localPokemon.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bl.get(1L))
                .isInstanceOfSatisfying(BallastException.class, ex -> assertThat(ex.getHttpStatus()).isEqualTo(404));
    }

    // ---------- update (US04) ----------

    @Test
    void updateReplacesProprietaryFields() {
        when(localPokemon.findById(7L)).thenReturn(Optional.of(storedPikachu(7L, 4)));
        when(localPokemon.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalPokemonResponse updated = bl.update(7L, new UpdateLocalPokemonRequest("Pika", "Kanto", List.of("Favorite"), 4L));

        assertThat(updated.localizedName()).isEqualTo("Pika");
        assertThat(updated.tags()).containsExactly("favorite");
        assertThat(updated.updatedAt()).isEqualTo(T1);
    }

    @Test
    void updateOfUnknownIdIsNotFound() {
        when(localPokemon.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bl.update(7L, new UpdateLocalPokemonRequest(null, null, null, 0L)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.LOCAL_POKEMON_NOT_FOUND.code()));
    }

    @Test
    void staleVersionIsAConflictAndNothingIsSaved() {
        when(localPokemon.findById(7L)).thenReturn(Optional.of(storedPikachu(7L, 5)));

        assertThatThrownBy(() -> bl.update(7L, new UpdateLocalPokemonRequest(null, null, null, 4L)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.STALE_VERSION.code()));
        verify(localPokemon, never()).save(any());
    }

    @Test
    void invalidCustomizationIsRejectedBeforeSaving() {
        when(localPokemon.findById(7L)).thenReturn(Optional.of(storedPikachu(7L, 0)));

        assertThatThrownBy(() -> bl.update(7L, new UpdateLocalPokemonRequest(null, null, List.of("not valid!"), 0L)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_CUSTOMIZATION.code()));
        verify(localPokemon, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesAnExistingPokemon() {
        when(localPokemon.existsById(1L)).thenReturn(true);

        bl.delete(1L);

        verify(localPokemon).deleteById(1L);
    }

    @Test
    void deleteOfUnknownIdIsNotFound() {
        when(localPokemon.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> bl.delete(1L)).isInstanceOf(BallastException.class);
        verify(localPokemon, never()).deleteById(anyLong());
    }
}
