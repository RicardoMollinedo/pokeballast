package com.ballastlane.pokeproject.bl;

import com.ballastlane.pokeproject.bl.model.PageQuery;
import com.ballastlane.pokeproject.bl.model.Task;
import com.ballastlane.pokeproject.bl.model.TaskContent;
import com.ballastlane.pokeproject.bl.model.TaskStatus;
import com.ballastlane.pokeproject.bl.model.User;
import com.ballastlane.pokeproject.bl.port.TaskPort;
import com.ballastlane.pokeproject.bl.port.UserPort;
import com.ballastlane.pokeproject.dto.CreateTaskRequest;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.TaskResponse;
import com.ballastlane.pokeproject.dto.UpdateTaskRequest;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/** Task management for the authenticated user. Every operation is scoped to the caller's own tasks. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskBl {

    private final TaskPort tasks;
    private final UserPort users;
    private final TaskMapper mapper;
    private final Clock clock;

    /**
     * Creates a task for the caller.
     *
     * <ol>
     *   <li>Resolves the owner from the token's username (4001 if the account no longer exists).</li>
     *   <li>Validates the content: title 1-120, description up to 2000, due date not in the past (1008, per field).</li>
     *   <li>Creates it with status TODO and saves it.</li>
     *   <li>Returns the response DTO.</li>
     * </ol>
     */
    public TaskResponse create(String username, CreateTaskRequest request) {
        long ownerId = ownerId(username);
        TaskContent content = TaskContent.of(request.title(), request.description(), request.dueDate(), today(), null);
        log.info("[BL] Creating task - ownerId={}", ownerId);
        return mapper.toResponse(tasks.save(Task.create(ownerId, content, clock.instant())));
    }

    /**
     * Lists the caller's tasks.
     *
     * <ol>
     *   <li>Validates the paging (1002) and the optional status filter (1008).</li>
     *   <li>Queries only the caller's tasks, optionally by status, ordered by due date (nulls last).</li>
     * </ol>
     */
    public PageResponse<TaskResponse> list(String username, String statusFilter, int page, int size) {
        PageQuery pageQuery = new PageQuery(page, size);
        TaskStatus status = statusFilter == null || statusFilter.isBlank() ? null : parseStatus(statusFilter);
        long ownerId = ownerId(username);
        log.info("[BL] Listing tasks - ownerId={}, status={}, page={}, size={}", ownerId, status, page, size);
        return mapper.toPage(tasks.findByOwner(ownerId, status, pageQuery));
    }

    /**
     * Returns one of the caller's tasks.
     *
     * <ol>
     *   <li>Loads it scoped by owner: another user's task is reported exactly like a missing one (2003, 404),
     *       so its existence is not revealed.</li>
     * </ol>
     */
    public TaskResponse get(String username, long id) {
        return mapper.toResponse(load(ownerId(username), id));
    }

    /**
     * Replaces title, description, status and due date of one of the caller's tasks.
     *
     * <ol>
     *   <li>Loads it scoped by owner (2003 otherwise).</li>
     *   <li>Rejects a stale {@code version} (3002, 409) to prevent lost updates.</li>
     *   <li>Validates the content; an unchanged past due date is accepted (1008 otherwise).</li>
     *   <li>Applies the status transition through the workflow table (3005, 409 if not allowed).</li>
     *   <li>Saves it (the version is also checked by the database) and returns the response DTO.</li>
     * </ol>
     */
    public TaskResponse update(String username, long id, UpdateTaskRequest request) {
        Task current = load(ownerId(username), id);
        if (current.version() != request.version()) {
            log.info("[BL] Rejecting stale task update - id={}, expected={}, actual={}", id, request.version(), current.version());
            throw new BallastException(BallastErrorCode.STALE_VERSION);
        }
        TaskContent content = TaskContent.of(request.title(), request.description(), request.dueDate(), today(),
                current.content().dueDate());
        TaskStatus newStatus = parseStatus(request.status());
        log.info("[BL] Updating task - id={}, status {} -> {}", id, current.status(), newStatus);
        return mapper.toResponse(tasks.save(current.update(content, newStatus, clock.instant())));
    }

    /**
     * Deletes one of the caller's tasks.
     *
     * <ol>
     *   <li>Deletes it scoped by owner in a single statement; nothing deleted means 2003 (404).</li>
     * </ol>
     */
    public void delete(String username, long id) {
        long ownerId = ownerId(username);
        if (!tasks.deleteByIdAndOwner(id, ownerId)) {
            throw new BallastException(BallastErrorCode.TASK_NOT_FOUND);
        }
        log.info("[BL] Task deleted - id={}, ownerId={}", id, ownerId);
    }

    private static TaskStatus parseStatus(String raw) {
        try {
            return TaskStatus.valueOf(raw.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BallastException(BallastErrorCode.INVALID_TASK, BallastErrorCode.INVALID_TASK.defaultMessage(),
                    java.util.Map.of("status", "must be TODO, IN_PROGRESS or DONE"));
        }
    }

    private Task load(long ownerId, long id) {
        return tasks.findByIdAndOwner(id, ownerId).orElseThrow(() -> new BallastException(BallastErrorCode.TASK_NOT_FOUND));
    }

    /** The owner always comes from the validated token, never from the request. */
    private long ownerId(String username) {
        return users.findByUsername(username).map(User::id)
                .orElseThrow(() -> new BallastException(BallastErrorCode.UNAUTHORIZED));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
