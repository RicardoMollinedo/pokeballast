package com.ballastlane.pokeproject.bl.port;

import com.ballastlane.pokeproject.bl.model.User;

import java.util.Optional;

/** Persistence of users. Implemented in {@code repository.adapter}. */
public interface UserPort {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * @throws com.ballastlane.pokeproject.exceptions.BallastException USERNAME_TAKEN (3004) if a concurrent
     *                                                                 registration took the username first
     */
    User save(User user);
}
