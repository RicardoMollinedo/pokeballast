package com.ballastlane.pokeproject.bl.model;

import java.time.Instant;

/** Signed bearer token and the instant it stops being valid. */
public record AccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "AccessToken[value=***, expiresAt=" + expiresAt + "]";
    }
}
