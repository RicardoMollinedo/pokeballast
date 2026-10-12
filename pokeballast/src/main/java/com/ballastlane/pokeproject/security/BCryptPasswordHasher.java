package com.ballastlane.pokeproject.security;

import com.ballastlane.pokeproject.bl.port.PasswordHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** {@link PasswordHasherPort} with BCrypt (Spring Security's encoder). */
@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    private final PasswordEncoder encoder;
    /** Compared against when the user does not exist, so both paths cost one BCrypt check. */
    private final String dummyHash;

    public BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (hash == null) {
            encoder.matches(rawPassword, dummyHash);
            return false;
        }
        return encoder.matches(rawPassword, hash);
    }
}
