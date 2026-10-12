package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.TaskBl;
import com.ballastlane.pokeproject.dto.BallastResponse;
import com.ballastlane.pokeproject.dto.CreateTaskRequest;
import com.ballastlane.pokeproject.dto.PageResponse;
import com.ballastlane.pokeproject.dto.TaskResponse;
import com.ballastlane.pokeproject.dto.UpdateTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Tasks of the authenticated user. Entry point only: the owner comes from the token (jwt.getSubject()). */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Personal task management")
public class TaskController {

    private final TaskBl taskBl;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(summary = "Create a task", security = @SecurityRequirement(name = "bearer"), description = """
            Creates a task for the caller with status TODO.

            **Validations:** title required, 1-120 characters (400, 1000/1008); description up to 2000 characters;
            due date not in the past (400, 1008, per field); unknown or malformed fields (400, 1001).

            **Role:** TRAINER or ADMIN.""")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Created; Location header"),
            @ApiResponse(responseCode = "400", description = "Invalid task (1000/1001/1008)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token (4001)")})
    public ResponseEntity<BallastResponse<TaskResponse>> create(@AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody CreateTaskRequest request) {
        log.info("[REQUEST] Create task - user={}, title={}, dueDate={}", jwt.getSubject(), request.title(), request.dueDate());
        TaskResponse body = taskBl.create(jwt.getSubject(), request);
        log.info("[REQUEST] Create task responded - id={}, status={}", body.id(), body.status());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(body.id()).toUri()).body(BallastResponse.success(body));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(summary = "List my tasks", security = @SecurityRequirement(name = "bearer"), description = """
            The caller's tasks only, ordered by due date (tasks without a date last).

            **Validations:** 0 <= page, 1 <= size <= 50 (400, 1002); status must be TODO, IN_PROGRESS or DONE (400, 1008).

            **Role:** TRAINER or ADMIN.""")
    public ResponseEntity<BallastResponse<PageResponse<TaskResponse>>> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Optional status filter: TODO, IN_PROGRESS or DONE") @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("[REQUEST] List tasks - user={}, status={}, page={}, size={}", jwt.getSubject(), status, page, size);
        PageResponse<TaskResponse> body = taskBl.list(jwt.getSubject(), status, page, size);
        log.info("[REQUEST] List tasks responded - total={}", body.totalElements());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(summary = "Get one of my tasks", security = @SecurityRequirement(name = "bearer"), description = """
            **Validations:** the task must exist and belong to the caller; otherwise 404 (2003), even if it belongs
            to another user (its existence is not revealed).

            **Role:** TRAINER or ADMIN.""")
    public ResponseEntity<BallastResponse<TaskResponse>> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        log.info("[REQUEST] Get task - user={}, id={}", jwt.getSubject(), id);
        TaskResponse body = taskBl.get(jwt.getSubject(), id);
        log.info("[REQUEST] Get task responded - id={}, status={}", body.id(), body.status());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(summary = "Update one of my tasks", security = @SecurityRequirement(name = "bearer"), description = """
            Full replacement of title, description, status and due date.

            **Validations:** ownership (404, 2003); version required and current (400 1000 / 409 3002); status change
            must follow TODO -> IN_PROGRESS -> DONE, IN_PROGRESS -> TODO, DONE -> TODO (409, 3005); content rules as
            on create, a past due date is only accepted if it did not change (400, 1008).

            **Role:** TRAINER or ADMIN.""")
    public ResponseEntity<BallastResponse<TaskResponse>> update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                                               @Valid @RequestBody UpdateTaskRequest request) {
        log.info("[REQUEST] Update task - user={}, id={}, status={}, dueDate={}, version={}", jwt.getSubject(), id,
                request.status(), request.dueDate(), request.version());
        TaskResponse body = taskBl.update(jwt.getSubject(), id, request);
        log.info("[REQUEST] Update task responded - id={}, status={}, version={}", body.id(), body.status(), body.version());
        return ResponseEntity.ok(BallastResponse.success(body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER','ADMIN')")
    @Operation(summary = "Delete one of my tasks", security = @SecurityRequirement(name = "bearer"), description = """
            204 No Content on success. **Validations:** ownership (404, 2003). **Role:** TRAINER or ADMIN.""")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        log.info("[REQUEST] Delete task - user={}, id={}", jwt.getSubject(), id);
        taskBl.delete(jwt.getSubject(), id);
        log.info("[REQUEST] Delete task responded - id={}, status=204", id);
        return ResponseEntity.noContent().build();
    }
}
