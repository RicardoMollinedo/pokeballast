package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Credentials of a new account, validated: username 3-30 letters, digits or underscore
 * (normalized to lower case, so uniqueness is case-insensitive); password 8-72 characters with at
 * least one letter and one digit (BCrypt ignores bytes beyond 72).
 */
public record UserCredentials(String username, String password) {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,30}$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 72;

    public UserCredentials {
        username = username == null ? null : username.strip().toLowerCase(Locale.ROOT);
        Map<String, String> errors = new LinkedHashMap<>();
        if (username == null || !USERNAME.matcher(username).matches()) {
            errors.put("username", "must be 3-30 characters: letters, digits or underscore");
        }
        if (password == null || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            errors.put("password", "must be between %d and %d characters".formatted(MIN_PASSWORD, MAX_PASSWORD));
        } else if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            errors.put("password", "must contain at least one letter and one digit");
        }
        if (!errors.isEmpty()) {
            throw new BallastException(BallastErrorCode.INVALID_USER_DATA,
                    BallastErrorCode.INVALID_USER_DATA.defaultMessage(), errors);
        }
    }

    @Override
    public String toString() {
        return "UserCredentials[username=" + username + ", password=***]";
    }
}
