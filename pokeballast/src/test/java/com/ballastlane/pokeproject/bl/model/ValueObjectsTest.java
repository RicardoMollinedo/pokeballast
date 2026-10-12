package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void pageQueryComputesTheOffset() {
        assertThat(new PageQuery(3, 20).offset()).isEqualTo(60);
    }

    @Test
    void pageQueryRejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new PageQuery(-1, PageQuery.MAX_SIZE + 1))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_PAGINATION.code());
                    assertThat(ex.getHttpStatus()).isEqualTo(400);
                    assertThat(ex.<Map<String, String>>getData()).containsOnlyKeys("page", "size");
                });
    }

    @Test
    void pageResultComputesTotalPagesAndMaps() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2), 0, 2, 5);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.map(i -> "#" + i).items()).containsExactly("#1", "#2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"25", "pikachu", " Mr-Mime ", "porygon2"})
    void identifierAcceptsIdsAndSlugs(String raw) {
        assertThat(PokemonIdentifier.of(raw).value()).isEqualTo(raw.strip().toLowerCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "0", "-1", "../etc/passwd", "pika chu", "a/b", "1234567"})
    void identifierRejectsAnythingElse(String raw) {
        assertThatThrownBy(() -> PokemonIdentifier.of(raw))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_POKEMON_IDENTIFIER.code()));
    }

    @Test
    void measurementsConvertToSiUnits() {
        BodyMeasurements measurements = new BodyMeasurements(7, 69);

        assertThat(measurements.heightMeters()).isEqualTo(0.7);
        assertThat(measurements.weightKilograms()).isEqualTo(6.9);
    }

    @Test
    void measurementsCannotBeNegative() {
        assertThatThrownBy(() -> new BodyMeasurements(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
