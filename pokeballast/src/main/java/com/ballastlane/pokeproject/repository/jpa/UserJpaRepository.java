package com.ballastlane.pokeproject.repository.jpa;

import com.ballastlane.pokeproject.repository.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    @Query("select u from UserEntity u where u.username = :username")
    Optional<UserEntity> findByUsername(@Param("username") String username);

    @Query("select case when count(u) > 0 then true else false end from UserEntity u where u.username = :username")
    boolean existsByUsername(@Param("username") String username);
}
