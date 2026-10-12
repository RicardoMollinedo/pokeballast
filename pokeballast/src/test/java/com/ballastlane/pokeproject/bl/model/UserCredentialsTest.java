package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCredentialsTest {

    @Test
    void normalizesTheUsernameToLowerCase() {
        assertThat(new UserCredentials("  Ash_Ketchum ", "Pikachu123").username()).isEqualTo("ash_ketchum");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "has space", "semi;colon", "abcdefghijabcdefghijabcdefghijk"})
    void rejectsInvalidUsernames(String username) {
        assertThatThrownBy(() -> new UserCredentials(username, "Pikachu123"))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_USER_DATA.code());
                    assertThat(ex.<Map<String, String>>getData()).containsOnlyKeys("username");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "onlyletters", "12345678"})
    void rejectsWeakPasswords(String password) {
        assertThatThrownBy(() -> new UserCredentials("ash", password))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.<Map<String, String>>getData()).containsOnlyKeys("password"));
    }

    @Test
    void rejectsPasswordsLongerThanBcryptSupports() {
        assertThatThrownBy(() -> new UserCredentials("ash", "a1".repeat(37))).isInstanceOf(BallastException.class);
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() {
        assertThatThrownBy(() -> new UserCredentials(null, null))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.<Map<String, String>>getData()).containsOnlyKeys("username", "password"));
    }

    @Test
    void neverExposesThePasswordInToString() {
        assertThat(new UserCredentials("ash", "Pikachu123").toString()).doesNotContain("Pikachu123");
    }
}
