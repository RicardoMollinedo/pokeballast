package com.ballastlane.pokeproject.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilTest {

    private final PokeApiUrlUtil urlUtil = new PokeApiUrlUtil();
    private final TextNormalizer textNormalizer = new TextNormalizer();
    private final LogSanitizer logSanitizer = new LogSanitizer();

    @Test
    void extractsTheIdFromPokeApiResourceUrls() {
        assertThat(urlUtil.extractId("https://pokeapi.co/api/v2/evolution-chain/67/")).isEqualTo(67);
        assertThat(urlUtil.extractId("https://pokeapi.co/api/v2/pokemon-species/25")).isEqualTo(25);
    }

    @Test
    void rejectsUrlsWithoutANumericId() {
        assertThatThrownBy(() -> urlUtil.extractId("https://pokeapi.co/api/v2/pokemon/pikachu/"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildsTheSpriteUrlOfASpecies() {
        assertThat(urlUtil.spriteUrl(25)).endsWith("/sprites/pokemon/25.png");
    }

    @Test
    void cleansFlavorTextControlCharacters() {
        assertThat(textNormalizer.cleanFlavorText("A strange seed was\nplanted on its\fback  at birth."))
                .isEqualTo("A strange seed was planted on its back at birth.");
        assertThat(textNormalizer.cleanFlavorText(null)).isNull();
    }

    @Test
    void sanitizesValuesBeforeLogging() {
        assertThat(logSanitizer.sanitize("ash\r\n[REQUEST] forged line")).doesNotContain("\n", "\r");
        assertThat(logSanitizer.sanitize("x".repeat(500))).hasSize(200);
        assertThat(logSanitizer.sanitize(null)).isNull();
    }
}
