# Generative AI tools: Task Management API

The exercise: write the prompt you would give a GenAI coding tool to generate a RESTful API for a simple task management system. It needs CRUD on tasks with `title`, `description`, `status` and `due_date`, each task belonging to a user (a basic User model already exists). Then show the output and explain how it was validated.

- **Tool:** Claude Code (CLI agent), run inside this repository, so it could read the project's own conventions: `pokeballast/CLAUDE.md` (package structure and dependency rules) and the team's endpoint conventions (thin controllers, business logic in `bl`, `BallastResponse`, `@PreAuthorize`, prefixed logs, Swagger, TDD).
- **Output:** [`task-api/`](task-api/) is a representative sample of the generated implementation: the migration, the business rules, the business logic, persistence, the controller and tests. It follows the same structure as `pokeballast/`, so it would plug into the same application.

---

## 1. The prompt

````text
You are a senior Java engineer working in this repository. Read pokeballast/CLAUDE.md first and follow
its package structure and dependency rules exactly (api / bl / bl.model / bl.port / dto / mapper /
repository.entity / repository.jpa / repository.adapter / exceptions).

## Goal
Add a RESTful API for a simple task management system to the existing Spring Boot 4 / Java 21 app.

## Domain
Task: id, title, description, status, dueDate, owner (the existing app_user), version, createdAt, updatedAt.
- title: required, trimmed, 1..120 characters.
- description: optional, max 2000 characters; blank becomes null.
- status: TODO | IN_PROGRESS | DONE. New tasks start as TODO. Allowed transitions:
  TODO -> IN_PROGRESS -> DONE, IN_PROGRESS -> TODO, DONE -> TODO (reopen). Anything else is a business
  error (409). Keeping the same status is allowed.
- dueDate: optional ISO date (yyyy-MM-dd). It cannot be in the past when it is set or changed
  (use the injected java.time.Clock so this is testable). An unchanged past due date is allowed on update.

## API (base path /api/v1/tasks, every endpoint requires an authenticated user, roles TRAINER or ADMIN)
- POST   /            create -> 201 + Location header
- GET    /            list my tasks, paginated (page >= 0, 1 <= size <= 50), optional ?status= filter,
                      ordered by dueDate (nulls last) then id
- GET    /{id}        one of my tasks
- PUT    /{id}        full update of title, description, status, dueDate; requires "version"
                      (optimistic locking -> 409 on mismatch)
- DELETE /{id}        204 No Content

## Hard requirements
- SECURITY: a user can only see and modify their own tasks. The owner comes from the JWT subject, never
  from the request body or a header. Another user's task must return 404 (not 403), so we don't reveal it
  exists. The ownership filter must be in the JPQL query, not applied in memory.
- Controllers are entry points only: no mapping, no business logic. The BL validates, applies the rules,
  calls the ports and returns the response DTO. Persistence only through a bl.port interface.
- Every response uses the existing BallastResponse envelope (DELETE: 204 without body). Errors are
  BallastException with new BallastErrorCode entries: invalid task (400, per-field messages in data),
  task not found (404), invalid status transition (409). Reuse STALE_VERSION and INVALID_PAGINATION.
- @PreAuthorize on every endpoint. Swagger @Operation on every endpoint stating the functionality,
  the validations and the role; @Schema on every DTO field (required/nullable).
- Logs with prefixes [REQUEST] / [BL] / [DB]; never log sensitive data.
- New Flyway script in pokeballast/database/scripts (next version number), FK to app_user with
  ON DELETE CASCADE, index on (owner_id, status), check constraint on status.
- Unknown JSON fields are already rejected with 400; do not change that.

## Process: TDD
Write the tests first, then the code:
1. Unit tests for the status transitions and the task rules (title, description, due date).
2. BL unit tests with mocked ports: create, update (transition, stale version, due date), not found
   for another user's task, delete.
3. @WebMvcTest for the controller: 201/200/204, 400 (body and business), 401 without token, 404, 409.
4. @DataJpaTest proving that the queries never return another user's tasks.

## Output
Generate the files in this order: migration, bl.model, bl.port, bl, dto, mapper, repository, api, tests.
Do not add dependencies. At the end, list your assumptions and anything you were unsure about.
````

Why the prompt looks like this:
- **It points to the project's own rules** (`CLAUDE.md`) instead of re-explaining them, so the output matches the existing architecture.
- **Business rules are written out** (transitions, the date rule, limits). Otherwise the model invents its own or leaves them out.
- **The security requirement is explicit and testable** ("404 for another user's task, filter in the query"). Missing ownership checks (IDOR) are the most common serious bug in generated CRUD code.
- **"Tests first" with specific cases.** Otherwise generated tests tend to cover only the happy path.
- **"List your assumptions"** makes the model's guesses visible so they can be reviewed.

---

## 2. The output

The generated code is in [`task-api/`](task-api/), following the repository layout:

| File | Content |
|---|---|
| `database/scripts/V3__create_task.sql` | Table, FK to `app_user`, check constraint, index |
| `bl/model/TaskStatus.java` | Status enum with the allowed transitions |
| `bl/model/Task.java`, `bl/model/TaskContent.java` | Immutable task, and the validated title/description/due date |
| `bl/port/TaskPort.java` | Persistence port, always scoped by owner |
| `bl/TaskBl.java` | Business logic: validation, ownership, transitions, optimistic locking, mapping to the DTO |
| `dto/*` | `CreateTaskRequest`, `UpdateTaskRequest`, `TaskResponse` with `@Schema` |
| `mapper/TaskMapper.java` | Model ↔ entity ↔ DTO |
| `repository/...` | JPA entity, JPQL repository (owner in every query), adapter |
| `api/TaskController.java` | Thin controller: `@PreAuthorize`, Swagger, `[REQUEST]` logs |
| `exceptions/BallastErrorCode-additions.txt` | The new error codes |
| `test/...` | Transitions, rules, BL with mocks, controller (`@WebMvcTest`), repository ownership (`@DataJpaTest`) |

**Verified:** the sample was copied over a copy of `pokeballast/`, together with the three new error codes. It compiles, and **its 33 tests pass**: rules 12, BL 9, controller 9, repository ownership 3. The existing **ArchUnit rules (13)** and the full application context (including the new `V3` migration) also stay green. It is kept apart from `pokeballast/` because it is the answer to this separate exercise, not a PokeBallast feature.

Assumptions reported by the tool:
1. The authenticated user is resolved from the JWT subject through the existing `UserPort.findByUsername`.
2. Tasks are private to their owner; an ADMIN does not see other users' tasks, because the requirement did not mention it.
3. `PUT` is a full replacement: omitted optional fields are cleared.
4. A past due date that does not change on update is allowed (otherwise old tasks could never be edited).

---

## 3. Validating the AI's suggestions

I treat AI output as a pull request from a fast but unreliable colleague: useful, never trusted by default.

- **Understand before accepting.** When the AI proposes something I didn't expect (for example, a design it says
  is better for performance), I ask it to justify the choice and check the claim against the official
  documentation. AI is a learning tool, but it can be confidently wrong: if the documentation disagrees,
  I give it the source and ask it to correct itself.
- **Verify with evidence, not with the AI's opinion.** Every change has to pass checks that don't depend on the AI:
  - **Tests written first.** They caught real bugs in generated code: a PokeAPI identifier validator that accepted
    `"0"`, and a JWT expiry reported with nanoseconds that did not match the token.
  - **ArchUnit rules.** They reject code that breaks the architecture. In this task, the first version used the
    `TaskStatus` enum in the controller and DTO, a layer violation that the build caught.
  - **Running the real thing.** I checked against the real PokeAPI, PostgreSQL, Docker and a real browser. This found
    problems no unit test showed: an expected 409 polluting the browser console, a layout overflowing on mobile,
    and audit data lost on worker threads.

## 4. Correcting or improving the output

I fix the cause, not only the symptom. When the output is wrong because the AI lacked context, I write the rule down
where the AI will read it next time:
- `CLAUDE.md` holds the project rules (package structure, dependency rules).
- Skills hold the conventions (endpoint conventions, frontend architecture).
- The agent definition holds the way of working (TDD, impact analysis before changing code).

Example: the generated code put the mapping in the controllers and the orchestration of several PokeAPI calls inside
the service. I corrected the design (thin controllers; the business logic combines calls; one external call per
service method), wrote it into the skill, `CLAUDE.md` and the agent, and added ArchUnit rules so it cannot come back.

## 5. Edge cases, authentication and validations

The prompt states them explicitly, and each one is covered by a test:
- **Authentication and authorization:**
  - The owner comes from the JWT, never from the body or a header. An `ownerId` field in the body is rejected (400).
  - Another user's task returns **404, not 403**, so its existence is not revealed (IDOR).
  - Ownership is filtered **inside the JPQL query**, not in memory. A repository test proves a user can neither read
    nor delete someone else's task.
- **Validation in two layers:** Bean Validation checks the request shape (required fields, types, unknown fields →
  400). The business rules live in the domain: title 1–120, description up to 2000, due date not in the past (but an
  unchanged past date is allowed on update), status transitions (`TODO → DONE` is a 409). All errors come back at
  once, one message per field.
- **Concurrency:** optimistic locking with `version`, so a stale update gets 409 instead of silently overwriting.
- **Edge cases tested:** blank title after trimming, a title of 121 characters, an unknown status, a past due date, a
  token of a deleted account (401), deleting someone else's task (404), and a status filter.
