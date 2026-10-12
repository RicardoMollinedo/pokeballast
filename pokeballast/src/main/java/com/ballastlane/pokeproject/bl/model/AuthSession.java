package com.ballastlane.pokeproject.bl.model;

/** Result of a successful login. */
public record AuthSession(User user, AccessToken token) {
}
