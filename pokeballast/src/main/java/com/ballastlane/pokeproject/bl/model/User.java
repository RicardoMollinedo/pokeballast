package com.ballastlane.pokeproject.bl.model;

import java.util.Objects;

/**
 * Application user. Only the password hash is kept; the raw password never leaves the login and
 * registration flows.
 *
 * @param id {@code null} until persisted
 */
public record User(Long id, String username, String passwordHash, Role role) {

    public User {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role");
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", role=" + role + "]"; // never the hash
    }
}
