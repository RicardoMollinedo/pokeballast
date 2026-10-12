package com.ballastlane.pokeproject.mapper;

import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.repository.entity.UserEntity;
import org.springframework.stereotype.Component;

/** JPA entity ↔ business model for users. */
@Component
public class UserEntityMapper {

    public User toModel(UserEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getRole());
    }

    public UserEntity toNewEntity(User user) {
        UserEntity entity = UserEntity.create();
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        return entity;
    }
}
