package com.ballastlane.pokeproject.bl.port;

import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.PageResult;
import com.ballastlane.pokeproject.bl.model.Task;
import com.ballastlane.pokeproject.bl.model.TaskStatus;

import java.util.Optional;

/**
 * Persistence of tasks. Every read and delete is scoped by owner, so the business logic cannot
 * accidentally load another user's task (IDOR).
 */
public interface TaskPort {

    Optional<Task> findByIdAndOwner(long id, long ownerId);

    /** @param status null means every status */
    PageResult<Task> findByOwner(long ownerId, TaskStatus status, PageQuery pageQuery);

    /**
     * @throws com.ballastlane.pokeproject.exceptions.BallastException STALE_VERSION (3002) on a version mismatch
     */
    Task save(Task task);

    /** @return false when no task with that id belongs to the owner */
    boolean deleteByIdAndOwner(long id, long ownerId);
}
