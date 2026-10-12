package com.ballastlane.pokeproject.bl.port;

/** One-way password hashing. Implemented in {@code security}. */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    /**
     * @param hash the stored hash, or {@code null} when the user does not exist: the implementation must
     *             still spend the same time and return {@code false}, so timing does not reveal valid usernames
     */
    boolean matches(String rawPassword, String hash);
}
