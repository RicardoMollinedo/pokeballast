package com.ballastlane.pokeproject.exceptions;

/**
 * Single catalog of response codes. The first digit is the family:
 * 0 success, 1 validation (400), 2 not found (404), 3 conflict (409), 4 auth (401/403),
 * 5 external service (503), 9 unexpected (500).
 */
public enum BallastErrorCode {

    INVALID_REQUEST("1000", "Invalid request", 400),
    MALFORMED_REQUEST("1001", "Malformed request", 400),
    INVALID_PAGINATION("1002", "Invalid pagination parameters", 400),
    INVALID_POKEMON_IDENTIFIER("1003", "The pokemon identifier must be a positive number or a slug like 'mr-mime'", 400),
    INVALID_CUSTOMIZATION("1004", "Invalid proprietary fields", 400),
    METHOD_NOT_ALLOWED("1005", "HTTP method not supported for this resource", 405),
    UNSUPPORTED_MEDIA_TYPE("1006", "Content type not supported, use application/json", 415),
    INVALID_USER_DATA("1007", "Invalid username or password format", 400),
    RESOURCE_NOT_FOUND("2000", "Resource not found", 404),
    POKEMON_NOT_FOUND("2001", "Pokemon not found", 404),
    LOCAL_POKEMON_NOT_FOUND("2002", "Local pokemon not found", 404),
    POKEMON_ALREADY_IMPORTED("3001", "Pokemon already stored locally", 409),
    STALE_VERSION("3002", "The record was modified by someone else, reload and retry", 409),
    CONCURRENT_MODIFICATION("3003", "The record was modified concurrently or already exists, reload and retry", 409),
    USERNAME_TAKEN("3004", "Username is already taken", 409),
    UNAUTHORIZED("4001", "Authentication required", 401),
    INVALID_CREDENTIALS("4002", "Invalid username or password", 401),
    ACCESS_DENIED("4003", "Access denied", 403),
    POKEAPI_UNAVAILABLE("5001", "PokeAPI is temporarily unavailable, please try again later", 503),
    UNEXPECTED("9999", "Unexpected error", 500);

    private final String code;
    private final String defaultMessage;
    private final int httpStatus;

    BallastErrorCode(String code, String defaultMessage, int httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
