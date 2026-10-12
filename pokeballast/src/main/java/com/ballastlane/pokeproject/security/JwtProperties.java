package com.ballastlane.pokeproject.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @param secret HMAC-SHA256 key, at least 32 bytes; must come from the environment ({@code JWT_SECRET})
 *               outside development
 * @param ttl    token lifetime
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 bytes long");
        }
        if (ttl == null) {
            ttl = Duration.ofHours(2);
        }
    }
}
