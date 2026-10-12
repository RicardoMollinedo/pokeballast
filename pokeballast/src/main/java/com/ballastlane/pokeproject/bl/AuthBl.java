package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.AuthSession;
import com.ballastlane.pokeproject.bl.model.Role;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.bl.model.UserCredentials;
import com.ballastlane.pokeproject.bl.port.PasswordHasherPort;
import com.ballastlane.pokeproject.bl.port.TokenIssuerPort;
import com.ballastlane.pokeproject.bl.port.UserPort;
import com.ballastlane.pokeproject.dto.AuthResponse;
import com.ballastlane.pokeproject.dto.CredentialsRequest;
import com.ballastlane.pokeproject.dto.UserResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.UserResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/** User registration and authentication (F05). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthBl {

    private final UserPort users;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final UserResponseMapper mapper;

    /**
     * Creates a TRAINER account.
     *
     * <ol>
     *   <li>Validates and normalizes the credentials ({@link UserCredentials}): username to lower case,
     *       password strength; otherwise throws {@code INVALID_USER_DATA} (1007, 400) with one message per field.</li>
     *   <li>Checks {@link UserPort#existsByUsername}; if taken, throws {@code USERNAME_TAKEN} (3004, 409).
     *       The DB unique constraint covers concurrent registrations.</li>
     *   <li>Hashes the password through {@link PasswordHasherPort#hash} (BCrypt); the raw password is never stored.</li>
     *   <li>Saves the user with role {@link Role#TRAINER}: self-registration never grants ADMIN.</li>
     *   <li>Returns the created user as the response DTO (never the password hash).</li>
     * </ol>
     *
     * @throws BallastException 1007 or 3004 as described above
     */
    public UserResponse register(CredentialsRequest request) {
        UserCredentials credentials = new UserCredentials(request.username(), request.password());
        if (users.existsByUsername(credentials.username())) {
            log.info("[BL] Username already taken, rejecting registration - username={}", credentials.username());
            throw new BallastException(BallastErrorCode.USERNAME_TAKEN);
        }
        log.info("[BL] Registering user - username={}, role={}", credentials.username(), Role.TRAINER);
        return mapper.toUser(users.save(
                new User(null, credentials.username(), passwordHasher.hash(credentials.password()), Role.TRAINER)));
    }

    /**
     * Authenticates a user and issues an access token.
     *
     * <ol>
     *   <li>If username or password is blank, fails with {@code INVALID_CREDENTIALS} (4002, 401).</li>
     *   <li>Loads the user by the lower-cased username through {@link UserPort#findByUsername}.</li>
     *   <li>Verifies the password with {@link PasswordHasherPort#matches}. When the user does not exist it still
     *       runs a check against no hash, so the response time does not reveal which usernames exist.</li>
     *   <li>Unknown user and wrong password fail the same way: {@code INVALID_CREDENTIALS} (4002, 401), same message.</li>
     *   <li>Issues a signed token through {@link TokenIssuerPort#issue} and returns it with the user as the response DTO.</li>
     * </ol>
     *
     * @throws BallastException 4002 as described above
     */
    public AuthResponse login(CredentialsRequest request) {
        String username = request.username();
        String password = request.password();
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new BallastException(BallastErrorCode.INVALID_CREDENTIALS);
        }
        String normalized = username.strip().toLowerCase(Locale.ROOT);
        Optional<User> user = users.findByUsername(normalized);
        boolean valid = passwordHasher.matches(password, user.map(User::passwordHash).orElse(null));
        if (user.isEmpty() || !valid) {
            log.info("[BL] Login rejected - username={}", normalized);
            throw new BallastException(BallastErrorCode.INVALID_CREDENTIALS);
        }
        log.info("[BL] Login accepted - username={}, role={}", normalized, user.get().role());
        return mapper.toAuth(new AuthSession(user.get(), tokenIssuer.issue(user.get())));
    }

    /**
     * The current user, as carried by the already validated token (no database lookup).
     *
     * <ol>
     *   <li>Maps the token's subject and role claim to the response DTO.</li>
     * </ol>
     */
    public UserResponse currentUser(String username, String role) {
        log.info("[BL] Reading current user from token - username={}, role={}", username, role);
        return new UserResponse(username, role);
    }
}
