package com.ballastlane.pokeproject.bl.port;

import com.ballastlane.pokeproject.bl.model.AccessToken;
import com.ballastlane.pokeproject.bl.model.User;

/** Issues signed access tokens. Implemented in {@code security}. */
public interface TokenIssuerPort {

    AccessToken issue(User user);
}
