package com.ballastlane.pokeproject.bl.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ballastlane.pokeproject.support.TestData.T0;
import static com.ballastlane.pokeproject.support.TestData.T1;
import static com.ballastlane.pokeproject.support.TestData.pikachuDetail;
import static com.ballastlane.pokeproject.support.TestData.simpleDetail;
import static com.ballastlane.pokeproject.support.TestData.storedPikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPokemonTest {

    @Test
    void importCopiesUpstreamDataWithEmptyCustomization() {
        LocalPokemon imported = LocalPokemon.importFrom(pikachuDetail(), T0);

        assertThat(imported.id()).isNull();
        assertThat(imported.pokeApiId()).isEqualTo(25);
        assertThat(imported.category()).isEqualTo("Mouse Pokémon");
        assertThat(imported.spriteUrl()).isEqualTo("sprite/25.png");
        assertThat(imported.abilities()).containsExactly("static", "lightning-rod");
        assertThat(imported.customization()).isEqualTo(Customization.empty());
        assertThat(imported.version()).isZero();
        assertThat(imported.syncedAt()).isEqualTo(T0);
    }

    @Test
    void refreshUpdatesUpstreamFieldsButKeepsProprietaryOnes() {
        LocalPokemon stored = storedPikachu(7L, 3);

        LocalPokemon refreshed = stored.refreshFrom(pikachuDetail(65), T1);

        assertThat(refreshed.measurements().weightHectograms()).isEqualTo(65);
        assertThat(refreshed.customization()).isEqualTo(stored.customization());
        assertThat(refreshed.id()).isEqualTo(7L);
        assertThat(refreshed.version()).isEqualTo(3);
        assertThat(refreshed.syncedAt()).isEqualTo(T1);
    }

    @Test
    void refreshRejectsDataOfAnotherPokemon() {
        LocalPokemon stored = storedPikachu(7L, 0);

        assertThatThrownBy(() -> stored.refreshFrom(simpleDetail(26, "raichu"), T1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customizeReplacesOnlyProprietaryFields() {
        LocalPokemon stored = storedPikachu(7L, 1);
        Customization changes = new Customization("ピカチュウ", "Johto", List.of("favorite"));

        LocalPokemon customized = stored.customize(changes, T1);

        assertThat(customized.customization()).isEqualTo(changes);
        assertThat(customized.name()).isEqualTo("pikachu");
        assertThat(customized.syncedAt()).isEqualTo(T0);
        assertThat(customized.updatedAt()).isEqualTo(T1);
    }
}
