package com.ballastlane.pokeproject.mapper;

import com.ballastlane.pokeproject.bl.model.AuthSession;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.dto.AuthResponse;
import com.ballastlane.pokeproject.dto.UserResponse;
import org.springframework.stereotype.Component;

/** Users and sessions → API DTOs (never exposes the password hash). */
@Component
public class UserResponseMapper {

    public UserResponse toUser(User user) {
        return new UserResponse(user.username(), user.role().name());
    }

    public AuthResponse toAuth(AuthSession session) {
        return new AuthResponse(session.token().value(), "Bearer", session.token().expiresAt(), toUser(session.user()));
    }
}
