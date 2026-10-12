package com.ballastlane.pokeproject.security;

import com.ballastlane.pokeproject.bl.model.AccessToken;
import com.ballastlane.pokeproject.bl.model.Role;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAdaptersTest {

    private static final JwtProperties PROPERTIES =
            new JwtProperties("a-test-secret-that-is-long-enough-1234567890", "pokeballast", Duration.ofHours(2));

    private final SecurityConfig config = new SecurityConfig();
    private final JwtDecoder decoder = config.jwtDecoder(PROPERTIES);

    // ---------- BCrypt ----------

    @Test
    void hashesWithBcryptAndVerifies() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

        String hash = hasher.hash("Pikachu123");

        assertThat(hash).startsWith("$2").doesNotContain("Pikachu123");
        assertThat(hasher.matches("Pikachu123", hash)).isTrue();
        assertThat(hasher.matches("wrong-pass1", hash)).isFalse();
    }

    @Test
    void usesAProductionStrengthCostOfAtLeast12() {
        String hash = new BCryptPasswordHasher(config.passwordEncoder()).hash("Pikachu123");

        // format: $2a$<cost>$<22-char salt><31-char hash>; the cost is a power of two: 2^12 rounds
        assertThat(hash).matches("^\\$2[aby]\\$12\\$[./A-Za-z0-9]{53}$");
    }

    @Test
    void theSamePasswordNeverProducesTheSameHash() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(config.passwordEncoder());

        // a random salt per hash: no rainbow tables, and equal passwords are not visible in the database
        assertThat(hasher.hash("Pikachu123")).isNotEqualTo(hasher.hash("Pikachu123"));
    }

    @Test
    void existingHashesWithALowerCostStillVerify() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(config.passwordEncoder());
        String legacyCost10 = new BCryptPasswordEncoder(10).encode("Pikachu123");

        assertThat(hasher.matches("Pikachu123", legacyCost10)).as("the cost is read from the stored hash").isTrue();
    }

    @Test
    void missingHashIsAlwaysFalse() {
        assertThat(new BCryptPasswordHasher(new BCryptPasswordEncoder()).matches("Pikachu123", null)).isFalse();
    }

    // ---------- JWT ----------

    @Test
    void issuedTokenIsAcceptedByTheApiDecoderWithRoleAndExpiry() {
        JwtTokenIssuer issuer = new JwtTokenIssuer(config.jwtEncoder(PROPERTIES), PROPERTIES, Clock.systemUTC());

        AccessToken token = issuer.issue(new User(1L, "admin", "hash", Role.ADMIN));
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getExpiresAt()).as("same instant the API reports").isEqualTo(token.expiresAt());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("pokeballast");
    }

    @Test
    void expiredTokenIsRejected() {
        Clock threeHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(3)), ZoneOffset.UTC);
        JwtTokenIssuer issuer = new JwtTokenIssuer(config.jwtEncoder(PROPERTIES), PROPERTIES, threeHoursAgo);

        String token = issuer.issue(new User(1L, "ash", "hash", Role.TRAINER)).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties other = new JwtProperties("another-secret-that-is-also-long-enough-1", "pokeballast", Duration.ofHours(2));
        String forged = new JwtTokenIssuer(config.jwtEncoder(other), other, Clock.systemUTC())
                .issue(new User(1L, "mallory", "hash", Role.ADMIN)).value();

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretsAreRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "x", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class);
    }
}
