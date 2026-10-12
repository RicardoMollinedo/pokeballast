package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomizationTest {

    @Test
    void normalizesTextsAndTags() {
        Customization customization = new Customization("  Pikachu ES  ", "   ", List.of(" Mascot ", "mascot", "kanto-01"));

        assertThat(customization.localizedName()).isEqualTo("Pikachu ES");
        assertThat(customization.region()).as("blank becomes null").isNull();
        assertThat(customization.tags()).as("trimmed, lower-cased, de-duplicated, order kept")
                .containsExactly("mascot", "kanto-01");
    }

    @Test
    void nullTagsMeansNoTags() {
        assertThat(new Customization(null, null, null).tags()).isEmpty();
    }

    @Test
    void tooLongTextsAreRejectedWithOneErrorPerField() {
        String tooLong = "x".repeat(Customization.MAX_TEXT_LENGTH + 1);

        assertThatThrownBy(() -> new Customization(tooLong, tooLong, List.of()))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_CUSTOMIZATION.code());
                    assertThat(ex.<Map<String, String>>getData()).containsOnlyKeys("localizedName", "region");
                });
    }

    @Test
    void malformedTagsAreRejected() {
        for (String bad : new String[]{"has space", "UPPER!", "-leading", "double--hyphen", "", "x".repeat(31)}) {
            assertThatThrownBy(() -> new Customization(null, null, List.of(bad)))
                    .as("tag '%s'", bad)
                    .isInstanceOf(BallastException.class);
        }
    }

    @Test
    void nullTagInsideTheListIsRejected() {
        List<String> tags = new ArrayList<>(Arrays.asList("ok", null));

        assertThatThrownBy(() -> new Customization(null, null, tags)).isInstanceOf(BallastException.class);
    }

    @Test
    void moreThanTheMaximumTagsIsRejected() {
        List<String> tags = IntStream.rangeClosed(0, Customization.MAX_TAGS).mapToObj(i -> "tag" + i).toList();

        assertThatThrownBy(() -> new Customization(null, null, tags))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.<Map<String, String>>getData()).containsKey("tags"));
    }
}
