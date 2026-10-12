package com.ballastlane.pokeproject.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Helpers for PokeAPI resource URLs. */
@Slf4j
@Component
public class PokeApiUrlUtil {

    private static final String SPRITE_URL_TEMPLATE =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png";

    /** PokeAPI links resources by URL: ".../evolution-chain/1/" gives 1. */
    public int extractId(String resourceUrl) {
        String trimmed = resourceUrl.endsWith("/") ? resourceUrl.substring(0, resourceUrl.length() - 1) : resourceUrl;
        String lastSegment = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        try {
            int id = Integer.parseInt(lastSegment);
            log.debug("[UTIL] Extracted id from PokeAPI url - url={}, id={}", resourceUrl, id);
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("PokeAPI url without a numeric id: " + resourceUrl, ex);
        }
    }

    /** Front sprite of a species, used for evolution nodes (PokeAPI does not include it in the chain). */
    public String spriteUrl(int speciesId) {
        return SPRITE_URL_TEMPLATE.formatted(speciesId);
    }
}
