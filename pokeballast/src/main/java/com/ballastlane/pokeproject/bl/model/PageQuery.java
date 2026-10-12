package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zero-based page request. The size cap protects our DB and PokeAPI's fair-use policy
 * (each listed pokemon costs two extra upstream calls).
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageQuery {
        Map<String, String> errors = new LinkedHashMap<>();
        if (page < 0) {
            errors.put("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            errors.put("size", "must be between 1 and " + MAX_SIZE);
        }
        if (!errors.isEmpty()) {
            throw new BallastException(BallastErrorCode.INVALID_PAGINATION,
                    BallastErrorCode.INVALID_PAGINATION.defaultMessage(), errors);
        }
    }

    public long offset() {
        return (long) page * size;
    }
}
