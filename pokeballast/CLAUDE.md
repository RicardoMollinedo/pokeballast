# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

PokeBallast is a Spring Boot REST API that integrates with the PokeAPI (https://pokeapi.co/docs/v2). It lets users browse Pokémon, replicate them into a local relational database and edit the local records. A React frontend in `../react-pokeballast` consumes the API (in development it reaches it through the Vite proxy on `/api`, so no CORS configuration is needed).

Implemented: user stories 1–4, registration/login (JWT) and the demo seed. Tests run on the schema only (`src/test/resources/config/application.properties` drops the seed location).

## Commands

Use the Maven wrapper (Windows: `.\mvnw.cmd`, Unix: `./mvnw`).

- Full build with tests, coverage report (`target/site/jacoco`) and ArchUnit rules: `.\mvnw.cmd clean verify`
- Build: `.\mvnw.cmd clean package`
- Run (in-memory H2): `.\mvnw.cmd spring-boot:run`
- Run against the local PostgreSQL: `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run` (from `pokeballast/`). It reads `config/application-local.yaml`, which is git-ignored and never packaged; `config/application-local.example.yaml` is the template.
- All tests: `.\mvnw.cmd test`
- Single test class: `.\mvnw.cmd test -Dtest=ClassName`
- Single test method: `.\mvnw.cmd test -Dtest=ClassName#methodName`

## Stack

- Java 21, Spring Boot 4.1.1 (uses the Boot 4 modular starters: `spring-boot-starter-webmvc`, `-flyway`, and the matching `*-test` starters)
- Spring Data JPA + Flyway migrations: scripts live in **`database/scripts`** (Maven packages them into the classpath at `db/migration`; see `database/README.md` for the rules), the table design in **`database/model`**, Spring Security, Bean Validation, springdoc-openapi 3.1.0 (Swagger UI), Lombok (configured as an annotation processor in `maven-compiler-plugin`)
- Database: in-memory H2 in PostgreSQL mode by default (zero setup); set `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` for PostgreSQL. Flyway owns the schema (`ddl-auto: validate`).
- PokeAPI client: `RestClient` with timeouts, Caffeine cache, and **Resilience4j** (`resilience4j-spring-boot4`) below the cache: rate limiter, bulkhead, retry (transient only) and circuit breaker, configured under `resilience4j.*.instances.pokeapi`. The US01 fan-out runs in the BL on virtual threads. Tests use recorded payloads in `src/test/resources/pokeapi` (never the network).
- Security: stateless JWT (HS256, `security.jwt.secret` / `JWT_SECRET`, at least 32 bytes) with roles from the `roles` claim; `@PreAuthorize` on every endpoint. Passwords use BCrypt with cost 12. TLS terminates at the nginx proxy; `server.forward-headers-strategy: framework`.

## Project structure

Besides `src/`, the repository has `database/` (`model/`: data model design; `scripts/`: SQL migrations). A Dockerfile that builds the jar must copy `database/` too, or the migrations will be missing from the image.

All code lives under `com.ballastlane.pokeproject`. Every class goes in one of these packages:

```
com.ballastlane.pokeproject
├── api/                  REST controllers (@RestController): HTTP in/out only, delegate to bl
│   ├── advice/           @RestControllerAdvice: maps every exception to a BallastResponse
│   └── filter/           servlet filters, e.g. the audit headers filter (X-Forwarded-For, x-user, x-latitude, x-longitude, x-channel)
├── bl/                   business logic, annotated with @Service: one class per use case or group of use cases (e.g. PokemonSyncBl)
│   ├── model/            domain models used by the business logic (plain Java, no JPA/Jackson)
│   └── port/             interfaces the business logic needs from the outside world
│                         (local Pokémon store, Pokémon catalog, users, password hasher, token issuer...)
├── dto/                  request/response DTOs exposed by the API, including the BallastResponse envelope
│   └── pokeapi/          wire DTOs of the external PokeAPI responses
├── service/              calls to external services (PokeAPI client: RestClient, timeouts, caching);
│                         implements the corresponding bl/port interfaces
├── repository/
│   ├── entity/           JPA entities (@Entity)
│   ├── jpa/              Spring Data interfaces; queries with @Query and JPQL
│   └── adapter/          implements the bl/port persistence interfaces using jpa/ + mapper/
├── security/             adapters of the security ports: BCrypt password hasher, JWT token issuer, JwtProperties
├── mapper/               conversions between types: entity ↔ model, model ↔ DTO, PokeAPI DTO ↔ model
├── util/                 stateless helpers, annotated with @Component (date conversion, unit conversion, log sanitizing...)
├── exceptions/           BallastException (code + message required, generic data optional) and the BallastErrorCode catalog
└── config/               @Configuration classes: security (JWT, @EnableMethodSecurity), cache, OpenAPI
```

Responsibilities: **controllers** are entry points only; the **BL** orchestrates, validates, combines service calls and maps to the response DTOs; each **service** method makes one external call and converts its output and errors; each **repository adapter** method makes one persistence operation. Dependency rules (they keep the business logic independent of data access and of external services):

| Package | May depend on | Must NOT depend on |
|---|---|---|
| `bl` (and its sub-packages) | `bl.*`, `dto` (request/response), `mapper`, `exceptions`, `util`; Spring's `@Service`, SLF4J | `api`, `service`, `repository`, `security`, `config`, `dto.pokeapi`; JPA, Jackson, any other Spring API |
| `api` | `bl`, `dto`, `exceptions`, `util` | `mapper`, `bl.model`, `bl.port`, `repository`, `service`, `security` |
| `service` | `bl.port`, `bl.model`, `exceptions`, `dto.pokeapi`, `mapper`, `util` | `api`, `repository`, `bl` classes (no orchestration) |
| `repository` | `bl.port`, `bl.model`, `exceptions`, `mapper`, `util` | `api`, `service`, `dto` |
| `mapper` | `dto`, `dto.pokeapi`, `repository.entity`, `bl.model`, `util` | `api`, `bl` classes, `service`, `repository.jpa` |
| `util` | JDK, SLF4J, Spring's `@Component` | every other project package |
| `exceptions` | JDK only (it must stay framework-free so `bl` can throw it) | everything else |
| `security` | `bl.port`, `bl.model`; Spring Security crypto/JWT | `api`, `service`, `repository`, `dto`, `mapper` |
| `config` | anything (composition root) | |

- `bl` classes are annotated with `@Service` and receive their dependencies through constructor injection. That annotation is the only Spring dependency allowed in `bl`. `bl` receives the request DTOs, applies the rules with `bl.model`, calls the outside world only through `bl.port` interfaces, and returns the response DTOs (mapped with `mapper`). When data comes from several external calls (e.g. the US01 enrichment: page + pokemon + species per item), **the BL combines them**, concurrently when they are independent.
- Controllers are entry points only: they receive params and the request DTO, call one BL method and wrap its result in `BallastResponse`. No mapping, no `bl.model`, no repositories or external services.
- Audit headers (`X-Forwarded-For`, `x-user`, `x-latitude`, `x-longitude`, `x-channel`) belong to *our* API's requests only. They are never propagated to external service calls.
- PokeAPI DTOs (`dto.pokeapi`) never leave `service`/`mapper`: they are mapped to `bl.model` first.
- Transactions (`@Transactional`) belong in `repository.adapter`, not in `api`.
- Every response, success or error, is a `BallastResponse { responseCode, response, errorDetail }`: `0000` + the BL result on success; the `BallastException` code, data and message on error. BL methods return the response DTO.
- Endpoint conventions (routes, `@PreAuthorize`, `[REQUEST]`/`[BL]`/`[SERVICE]`/`[DB]`/`[UTIL]` logs without sensitive data, Swagger, BL Javadoc, audit headers, error codes) are defined in the workspace skill `spring-boot-endpoint` (`../.claude/skills/spring-boot-endpoint/`).
- Enforce these rules with an ArchUnit test and keep it green.

## Scope

User stories:
1. **Paginated list** of Pokémon showing sprite, category, weight and abilities, with PokeAPI responses cached.
2. **Detail view**: image, base stats, flavor-text description and evolution chain. This needs PokeAPI's `pokemon`, `pokemon-species` and `evolution-chain` endpoints.
3. **Sync/persist** Pokémon into the local DB, with room for custom fields (localized name, geographic metadata, internal tags).
4. **Update** locally stored Pokémon: return 404 for missing records and 400 for invalid payloads.

Technical requirements:
- **Clean Architecture**: the business logic layer must not depend on the web or persistence layers. See "Project structure" for the packages and their dependency rules.
- Full CRUD with standard HTTP verbs and a consistent response/error structure.
- User registration and authentication API with public and protected routes, plus a user table in the DB.
- TDD with unit tests for every core component.
- Seeded demo data and demo credentials, for example through a Flyway migration.
- A Dockerfile, a README (setup and technical docs) and a responsive frontend (React/Vue) that consumes the API and produces no browser-console warnings.

## Repository rules

- Include only this project's code, config and docs: no unrelated files or unnecessary boilerplate.
- **No symlinks anywhere in the repo.** The project lives in OneDrive, so check that committed files are real files.
