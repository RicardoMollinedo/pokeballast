package com.ballastlane.pokeproject.repository.adapter;

import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.bl.port.UserPort;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.UserEntityMapper;
import com.ballastlane.pokeproject.repository.entity.UserEntity;
import com.ballastlane.pokeproject.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** {@link UserPort} on top of Spring Data JPA. Logs usernames only, never password hashes. */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdapter implements UserPort {

    private final UserJpaRepository jpa;
    private final UserEntityMapper mapper;

    @Override
    public Optional<User> findByUsername(String username) {
        log.info("[DB] Finding user - username={}", username);
        Optional<User> user = jpa.findByUsername(username).map(mapper::toModel);
        log.info("[DB] Find user returned - username={}, found={}, role={}", username, user.isPresent(),
                user.map(User::role).orElse(null));
        return user;
    }

    @Override
    public boolean existsByUsername(String username) {
        log.info("[DB] Checking username exists - username={}", username);
        boolean exists = jpa.existsByUsername(username);
        log.info("[DB] Username exists check returned - username={}, exists={}", username, exists);
        return exists;
    }

    @Override
    @Transactional
    public User save(User user) {
        log.info("[DB] Saving user - username={}, role={}", user.username(), user.role());
        try {
            UserEntity saved = jpa.saveAndFlush(mapper.toNewEntity(user));
            log.info("[DB] Save user returned - id={}, username={}", saved.getId(), saved.getUsername());
            return mapper.toModel(saved);
        } catch (DataIntegrityViolationException ex) {
            // a concurrent registration took the username after the business check
            log.warn("[DB] Save user rejected by the database - username={}", user.username());
            throw new BallastException(BallastErrorCode.USERNAME_TAKEN);
        }
    }
}
