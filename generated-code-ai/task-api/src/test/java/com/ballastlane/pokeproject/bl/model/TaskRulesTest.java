package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @ParameterizedTest
    @CsvSource({"TODO,IN_PROGRESS", "IN_PROGRESS,DONE", "IN_PROGRESS,TODO", "DONE,TODO", "TODO,TODO", "DONE,DONE"})
    void allowedTransitions(TaskStatus from, TaskStatus to) {
        assertThat(from.transitionTo(to)).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({"TODO,DONE", "DONE,IN_PROGRESS"})
    void forbiddenTransitionsAreAConflict(TaskStatus from, TaskStatus to) {
        assertThatThrownBy(() -> from.transitionTo(to))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_STATUS_TRANSITION.code());
                    assertThat(ex.getHttpStatus()).isEqualTo(409);
                });
    }

    @Test
    void contentIsTrimmedAndBlankDescriptionBecomesNull() {
        TaskContent content = TaskContent.of("  Catch Pikachu ", "   ", TODAY, TODAY, null);

        assertThat(content.title()).isEqualTo("Catch Pikachu");
        assertThat(content.description()).isNull();
    }

    @Test
    void reportsEveryInvalidFieldAtOnce() {
        assertThatThrownBy(() -> TaskContent.of(" ", "x".repeat(2001), TODAY.minusDays(1), TODAY, null))
                .isInstanceOfSatisfying(BallastException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(BallastErrorCode.INVALID_TASK.code());
                    assertThat(ex.<Map<String, String>>getData()).containsOnlyKeys("title", "description", "dueDate");
                });
    }

    @Test
    void titleLongerThan120IsRejected() {
        assertThatThrownBy(() -> TaskContent.of("x".repeat(121), null, null, TODAY, null))
                .isInstanceOf(BallastException.class);
    }

    @Test
    void anUnchangedPastDueDateIsAcceptedOnUpdate() {
        LocalDate lastWeek = TODAY.minusDays(7);

        assertThat(TaskContent.of("Old task", null, lastWeek, TODAY, lastWeek).dueDate()).isEqualTo(lastWeek);
        assertThatThrownBy(() -> TaskContent.of("Old task", null, lastWeek.minusDays(1), TODAY, lastWeek))
                .as("moving it to another past date is not").isInstanceOf(BallastException.class);
    }
}
