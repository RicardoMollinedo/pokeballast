package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Proprietary fields that PokeAPI does not have (US03): a localized name, geographical metadata
 * and internal classification tags. They are the only editable fields (US04); replicated data is
 * mastered by PokeAPI and refreshed on re-sync.
 */
public record Customization(String localizedName, String region, List<String> tags) {

    public static final int MAX_TEXT_LENGTH = 60;
    public static final int MAX_TAGS = 10;
    private static final int MAX_TAG_LENGTH = 30;
    private static final Pattern TAG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    public Customization {
        Map<String, String> errors = new LinkedHashMap<>();
        localizedName = normalizeText("localizedName", localizedName, errors);
        region = normalizeText("region", region, errors);
        tags = normalizeTags(tags, errors);
        if (!errors.isEmpty()) {
            throw new BallastException(BallastErrorCode.INVALID_CUSTOMIZATION,
                    BallastErrorCode.INVALID_CUSTOMIZATION.defaultMessage(), errors);
        }
    }

    public static Customization empty() {
        return new Customization(null, null, List.of());
    }

    private static String normalizeText(String field, String value, Map<String, String> errors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            errors.put(field, "must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }

    private static List<String> normalizeTags(List<String> rawTags, Map<String, String> errors) {
        if (rawTags == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : rawTags) {
            String tag = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
            if (tag.isEmpty() || tag.length() > MAX_TAG_LENGTH || !TAG.matcher(tag).matches()) {
                errors.put("tags", "each tag must be 1-" + MAX_TAG_LENGTH
                        + " characters of lowercase letters, digits and single hyphens");
                return List.of();
            }
            unique.add(tag);
        }
        if (unique.size() > MAX_TAGS) {
            errors.put("tags", "at most " + MAX_TAGS + " tags are allowed");
        }
        return List.copyOf(unique);
    }
}
