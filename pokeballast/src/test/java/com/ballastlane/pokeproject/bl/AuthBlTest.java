package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.AccessToken;
import com.ballastlane.pokeproject.bl.model.Role;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.bl.port.PasswordHasherPort;
import com.ballastlane.pokeproject.bl.port.TokenIssuerPort;
import com.ballastlane.pokeproject.bl.port.UserPort;
import com.ballastlane.pokeproject.dto.AuthResponse;
import com.ballastlane.pokeproject.dto.CredentialsRequest;
import com.ballastlane.pokeproject.dto.UserResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.UserResponseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthBlTest {

    private static final User ASH = new User(1L, "ash", "hashed", Role.TRAINER);

    @Mock
    private UserPort users;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private TokenIssuerPort tokenIssuer;
    private AuthBl bl;

    @BeforeEach
    void setUp() {
        bl = new AuthBl(users, passwordHasher, tokenIssuer, new UserResponseMapper());
    }

    // ---------- register ----------

    @Test
    void registerStoresTheHashWithTheTrainerRole() {
        when(users.existsByUsername("ash")).thenReturn(false);
        when(passwordHasher.hash("Pikachu123")).thenReturn("hashed");
        when(users.save(any())).thenAnswer(inv -> {
            User toSave = inv.getArgument(0);
            return new User(7L, toSave.username(), toSave.passwordHash(), toSave.role());
        });

        UserResponse created = bl.register(new CredentialsRequest(" ASH ", "Pikachu123"));

        assertThat(created.username()).isEqualTo("ash");
        assertThat(created.role()).as("self-registration never grants ADMIN").isEqualTo("TRAINER");
        verify(users).save(new User(null, "ash", "hashed", Role.TRAINER));
    }

    @Test
    void registerRejectsATakenUsername() {
        when(users.existsByUsername("ash")).thenReturn(true);

        assertThatThrownBy(() -> bl.register(new CredentialsRequest("Ash", "Pikachu123")))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.USERNAME_TAKEN.code());
                    assertThat(ex.getHttpStatus()).isEqualTo(409);
                });
        verify(users, never()).save(any());
    }

    @Test
    void registerValidatesBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> bl.register(new CredentialsRequest("ash", "weak"))).isInstanceOf(BallastException.class);
        verifyNoInteractions(users, passwordHasher);
    }

    // ---------- login ----------

    @Test
    void loginIssuesATokenForValidCredentials() {
        AccessToken token = new AccessToken("jwt", Instant.parse("2030-01-01T00:00:00Z"));
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("Pikachu123", "hashed")).thenReturn(true);
        when(tokenIssuer.issue(ASH)).thenReturn(token);

        AuthResponse session = bl.login(new CredentialsRequest(" Ash ", "Pikachu123"));

        assertThat(session.accessToken()).isEqualTo("jwt");
        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.expiresAt()).isEqualTo(token.expiresAt());
        assertThat(session.user()).isEqualTo(new UserResponse("ash", "TRAINER"));
    }

    @Test
    void wrongPasswordAndUnknownUserFailTheSameWay() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(passwordHasher.matches("wrong-pass1", "hashed")).thenReturn(false);
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        BallastException wrongPassword = catchBallast(() -> bl.login(new CredentialsRequest("ash", "wrong-pass1")));
        BallastException unknownUser = catchBallast(() -> bl.login(new CredentialsRequest("ghost", "Whatever123")));

        assertThat(wrongPassword.getCode()).isEqualTo(BallastErrorCode.INVALID_CREDENTIALS.code())
                .isEqualTo(unknownUser.getCode());
        assertThat(wrongPassword.getMessage()).isEqualTo(unknownUser.getMessage());
        assertThat(wrongPassword.getHttpStatus()).isEqualTo(401);
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void unknownUserStillRunsAPasswordCheckSoTimingDoesNotRevealIt() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        catchBallast(() -> bl.login(new CredentialsRequest("ghost", "Whatever123")));

        verify(passwordHasher).matches(anyString(), isNull());
    }

    @Test
    void currentUserIsBuiltFromTheTokenClaims() {
        assertThat(bl.currentUser("admin", "ADMIN")).isEqualTo(new UserResponse("admin", "ADMIN"));
    }

    @Test
    void blankCredentialsAreInvalidCredentials() {
        BallastException ex = catchBallast(() -> bl.login(new CredentialsRequest(null, null)));

        assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_CREDENTIALS.code());
    }

    private static BallastException catchBallast(Runnable call) {
        try {
            call.run();
        } catch (BallastException ex) {
            return ex;
        }
        throw new AssertionError("Expected a BallastException");
    }
}
