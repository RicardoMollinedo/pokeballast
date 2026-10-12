package com.ballastlane.pokeproject.repository.adapter;

import com.ballastlane.pokeproject.bl.model.Role;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.UserEntityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// own in-memory database: data committed by @SpringBootTest flows must not leak into these assertions
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@Import({UserAdapter.class, UserEntityMapper.class})
class UserAdapterTest {

    @Autowired
    private UserAdapter adapter;

    @Test
    void savesAndFindsAUserByUsername() {
        User saved = adapter.save(new User(null, "misty", "$2a$10$hash", Role.TRAINER));

        assertThat(saved.id()).isNotNull();
        assertThat(adapter.findByUsername("misty")).get().satisfies(found -> {
            assertThat(found.passwordHash()).isEqualTo("$2a$10$hash");
            assertThat(found.role()).isEqualTo(Role.TRAINER);
        });
        assertThat(adapter.existsByUsername("misty")).isTrue();
        assertThat(adapter.existsByUsername("brock")).isFalse();
        assertThat(adapter.findByUsername("brock")).isEmpty();
    }

    @Test
    void duplicatedUsernameIsRejectedByTheDatabase() {
        adapter.save(new User(null, "misty", "h1", Role.TRAINER));

        assertThatThrownBy(() -> adapter.save(new User(null, "misty", "h2", Role.ADMIN)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.USERNAME_TAKEN.code()));
    }
}
