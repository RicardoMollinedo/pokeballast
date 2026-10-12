package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Task workflow. The transition table is the single source of truth for status changes. */
public enum TaskStatus {
    TODO, IN_PROGRESS, DONE;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = Map.of(
            TODO, EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(DONE, TODO),
            DONE, EnumSet.of(TODO)); // reopen

    /** Keeping the same status is always allowed (a PUT that only edits the title). */
    public boolean canTransitionTo(TaskStatus target) {
        return this == target || ALLOWED.get(this).contains(target);
    }

    public TaskStatus transitionTo(TaskStatus target) {
        if (!canTransitionTo(target)) {
            throw new BallastException(BallastErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot change a task from %s to %s".formatted(this, target), null);
        }
        return target;
    }
}
