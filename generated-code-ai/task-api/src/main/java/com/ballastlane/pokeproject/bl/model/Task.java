package com.ballastlane.pokeproject.bl.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A task owned by one user. Immutable: every change returns a new instance.
 *
 * @param id      null until persisted
 * @param version optimistic-locking version
 */
public record Task(Long id, long ownerId, TaskContent content, TaskStatus status, long version,
                   Instant createdAt, Instant updatedAt) {

    public Task {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(status, "status");
    }

    public static Task create(long ownerId, TaskContent content, Instant now) {
        return new Task(null, ownerId, content, TaskStatus.TODO, 0, now, now);
    }

    /** Full replacement; the status change must follow the workflow (409 otherwise). */
    public Task update(TaskContent newContent, TaskStatus newStatus, Instant now) {
        return new Task(id, ownerId, newContent, status.transitionTo(newStatus), version, createdAt, now);
    }
}
