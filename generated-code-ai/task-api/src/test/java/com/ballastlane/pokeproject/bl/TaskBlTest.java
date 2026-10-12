package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.model.Role;
import com.ballastlane.pokeproject.bl.model.Task;
import com.ballastlane.pokeproject.bl.model.TaskContent;
import com.ballastlane.pokeproject.bl.model.TaskStatus;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.bl.port.TaskPort;
import com.ballastlane.pokeproject.bl.port.UserPort;
import com.ballastlane.pokeproject.dto.CreateTaskRequest;
import com.ballastlane.pokeproject.dto.TaskResponse;
import com.ballastlane.pokeproject.dto.UpdateTaskRequest;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.TaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskBlTest {

    private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final User ASH = new User(7L, "ash", "hash", Role.TRAINER);

    @Mock
    private TaskPort tasks;
    @Mock
    private UserPort users;
    private TaskBl bl;

    @BeforeEach
    void setUp() {
        bl = new TaskBl(tasks, users, new TaskMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Task stored(TaskStatus status, long version) {
        return new Task(3L, 7L, new TaskContent("Catch Pikachu", null, TODAY.plusDays(1)), status, version, NOW, NOW);
    }

    @Test
    void createsATodoTaskOwnedByTheCaller() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return new Task(3L, t.ownerId(), t.content(), t.status(), 0, t.createdAt(), t.updatedAt());
        });

        TaskResponse created = bl.create("ash", new CreateTaskRequest(" Catch Pikachu ", null, TODAY));

        assertThat(created.status()).isEqualTo("TODO");
        assertThat(created.title()).isEqualTo("Catch Pikachu");
        verify(tasks).save(any(Task.class));
    }

    @Test
    void anotherUsersTaskIsReportedAsNotFound() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.findByIdAndOwner(99L, 7L)).thenReturn(Optional.empty()); // it exists, but for another owner

        assertThatThrownBy(() -> bl.get("ash", 99L))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.TASK_NOT_FOUND.code()));
    }

    @Test
    void updateFollowsTheWorkflow() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.findByIdAndOwner(3L, 7L)).thenReturn(Optional.of(stored(TaskStatus.TODO, 1)));
        when(tasks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse updated = bl.update("ash", 3L,
                new UpdateTaskRequest("Catch Pikachu", "with a Great Ball", "in_progress", TODAY.plusDays(1), 1L));

        assertThat(updated.status()).isEqualTo("IN_PROGRESS");
        assertThat(updated.description()).isEqualTo("with a Great Ball");
    }

    @Test
    void skippingAStepIsAConflictAndNothingIsSaved() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.findByIdAndOwner(3L, 7L)).thenReturn(Optional.of(stored(TaskStatus.TODO, 1)));

        assertThatThrownBy(() -> bl.update("ash", 3L, new UpdateTaskRequest("Catch Pikachu", null, "DONE", null, 1L)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_STATUS_TRANSITION.code()));
        verify(tasks, never()).save(any());
    }

    @Test
    void staleVersionIsRejected() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.findByIdAndOwner(3L, 7L)).thenReturn(Optional.of(stored(TaskStatus.TODO, 2)));

        assertThatThrownBy(() -> bl.update("ash", 3L, new UpdateTaskRequest("x", null, "TODO", null, 1L)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.STALE_VERSION.code()));
    }

    @Test
    void unknownStatusIsAFieldError() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.findByIdAndOwner(3L, 7L)).thenReturn(Optional.of(stored(TaskStatus.TODO, 1)));

        assertThatThrownBy(() -> bl.update("ash", 3L, new UpdateTaskRequest("x", null, "FINISHED", null, 1L)))
                .isInstanceOfSatisfying(BallastException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_TASK.code()));
    }

    @Test
    void listIsScopedToTheCallerAndFiltersByStatus() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.findByOwner(7L, TaskStatus.DONE, new PageQuery(0, 20)))
                .thenReturn(new PageResult<>(List.of(stored(TaskStatus.DONE, 0)), 0, 20, 1));

        assertThat(bl.list("ash", "done", 0, 20).content()).singleElement()
                .satisfies(task -> assertThat(task.status()).isEqualTo("DONE"));
    }

    @Test
    void deletingSomeoneElsesTaskIsNotFound() {
        when(users.findByUsername("ash")).thenReturn(Optional.of(ASH));
        when(tasks.deleteByIdAndOwner(99L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> bl.delete("ash", 99L)).isInstanceOf(BallastException.class);
    }

    @Test
    void aTokenOfADeletedAccountIsUnauthorized() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bl.list("ghost", null, 0, 20))
                .isInstanceOfSatisfying(BallastException.class, ex -> assertThat(ex.getHttpStatus()).isEqualTo(401));
    }
}
