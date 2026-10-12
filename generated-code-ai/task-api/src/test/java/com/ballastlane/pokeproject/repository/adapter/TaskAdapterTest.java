package com.ballastlane.pokeproject.repository.adapter;

import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.Role;
import com.ballastlane.pokeproject.bl.model.Task;
import com.ballastlane.pokeproject.bl.model.TaskContent;
import com.ballastlane.pokeproject.bl.model.TaskStatus;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.mapper.TaskMapper;
import com.ballastlane.pokeproject.mapper.UserEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Ownership is enforced by the JPQL queries themselves, against the real Flyway schema. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:task-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@Import({TaskAdapter.class, TaskMapper.class, UserAdapter.class, UserEntityMapper.class})
class TaskAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");

    @Autowired
    private TaskAdapter tasks;
    @Autowired
    private UserAdapter users;
    private long ash;
    private long misty;
    private Task mistysTask;

    @BeforeEach
    void setUp() {
        ash = users.save(new User(null, "ash", "h", Role.TRAINER)).id();
        misty = users.save(new User(null, "misty", "h", Role.TRAINER)).id();
        tasks.save(Task.create(ash, new TaskContent("No date", null, null), NOW));
        tasks.save(Task.create(ash, new TaskContent("Soon", null, LocalDate.of(2026, 10, 20)), NOW));
        mistysTask = tasks.save(Task.create(misty, new TaskContent("Misty's secret", null, null), NOW));
    }

    @Test
    void aUserNeverSeesAnotherUsersTask() {
        assertThat(tasks.findByIdAndOwner(mistysTask.id(), ash)).isEmpty();
        assertThat(tasks.findByIdAndOwner(mistysTask.id(), misty)).isPresent();
        assertThat(tasks.findByOwner(ash, null, new PageQuery(0, 50)).items())
                .extracting(t -> t.content().title()).containsExactly("Soon", "No date"); // due date first, nulls last
    }

    @Test
    void aUserCannotDeleteAnotherUsersTask() {
        assertThat(tasks.deleteByIdAndOwner(mistysTask.id(), ash)).isFalse();
        assertThat(tasks.findByIdAndOwner(mistysTask.id(), misty)).isPresent();
        assertThat(tasks.deleteByIdAndOwner(mistysTask.id(), misty)).isTrue();
    }

    @Test
    void filtersByStatus() {
        Task soon = tasks.findByOwner(ash, null, new PageQuery(0, 50)).items().getFirst();
        tasks.save(soon.update(soon.content(), TaskStatus.IN_PROGRESS, NOW));

        assertThat(tasks.findByOwner(ash, TaskStatus.IN_PROGRESS, new PageQuery(0, 50)).items())
                .extracting(t -> t.content().title()).containsExactly("Soon");
    }
}
