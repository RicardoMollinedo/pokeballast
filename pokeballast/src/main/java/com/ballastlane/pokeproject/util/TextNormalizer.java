package com.ballastlane.pokeproject.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class TextNormalizer {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\f\\n\\r\\u00ad]+");
    private static final Pattern REPEATED_SPACES = Pattern.compile("\\s{2,}");

    /** PokeAPI flavor texts come from game cartridges and contain line and page breaks (\n, \f). */
    public String cleanFlavorText(String raw) {
        if (raw == null) {
            return null;
        }
        String clean = REPEATED_SPACES.matcher(CONTROL_CHARACTERS.matcher(raw).replaceAll(" ")).replaceAll(" ").strip();
        log.debug("[UTIL] Cleaned flavor text - length={}", clean.length());
        return clean;
    }
}
