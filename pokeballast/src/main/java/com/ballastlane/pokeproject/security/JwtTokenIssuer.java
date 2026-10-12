package com.ballastlane.pokeproject.security;

import com.ballastlane.pokeproject.bl.model.AccessToken;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.bl.port.TokenIssuerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** {@link TokenIssuerPort}: HS256 JWT with issuer, subject (username), expiry and the "roles" claim. */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuerPort {

    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    @Override
    public AccessToken issue(User user) {
        // JWT dates have second precision: truncate so the expiry we report equals the one in the token
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(properties.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLES_CLAIM, List.of(user.role().name()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
