package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A PokeAPI id ("25") or slug ("pikachu", "mr-mime"). Validated here so user input is
 * never forwarded unchecked into an upstream URL.
 */
public record PokemonIdentifier(String value) {

    private static final Pattern NUMERIC = Pattern.compile("^[1-9]\\d{0,5}$");
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final int MAX_LENGTH = 50;

    public PokemonIdentifier {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        boolean allDigits = !normalized.isEmpty() && normalized.chars().allMatch(Character::isDigit);
        // digits-only input is an id and must be a valid one ("0" is not a slug)
        boolean valid = allDigits ? NUMERIC.matcher(normalized).matches() : SLUG.matcher(normalized).matches();
        if (normalized.length() > MAX_LENGTH || !valid) {
            throw new BallastException(BallastErrorCode.INVALID_POKEMON_IDENTIFIER);
        }
        value = normalized;
    }

    public static PokemonIdentifier of(String raw) {
        return new PokemonIdentifier(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
