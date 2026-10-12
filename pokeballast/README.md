# PokeBallast API (Spring Boot)

The backend of [PokeBallast](../README.md). The architecture, API conventions and design decisions are in the main README. This file covers backend-only tasks.

## Requirements

* JDK 21. Maven is not needed: the wrapper `./mvnw` downloads it.
* Optional: PostgreSQL. By default the app uses in-memory H2 in PostgreSQL mode.

## Commands

```bash
./mvnw spring-boot:run                       # http://localhost:8080 (H2 + demo data)
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run   # your PostgreSQL, see config/application-local.example.yaml
./mvnw clean verify                          # all tests + JaCoCo (target/site/jacoco) + ArchUnit rules
./mvnw test -Dtest=ClassName#method          # a single test
docker build -t pokeballast-api .            # image (multi-stage, non-root)
```

## Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | H2 in memory | JDBC connection, e.g. `jdbc:postgresql://localhost:5432/pokeballast` |
| `JWT_SECRET` | dev-only value | HS256 key, **at least 32 bytes**; the app refuses to start otherwise |
| `POKEAPI_BASE_URL` | `https://pokeapi.co/api/v2` | Upstream API |

## Where things live

| Concern | Location |
|---|---|
| Package structure and dependency rules | [`CLAUDE.md`](CLAUDE.md) ("Project structure"), enforced by `ArchitectureTest` |
| Business rules | `bl/model` (`Customization`, `PokemonIdentifier`, `PageQuery`, `UserCredentials`, `LocalPokemon`) |
| Error codes | `exceptions/BallastErrorCode` |
| Data model and SQL | [`database/`](database/): `model/` (design), `scripts/` (Flyway migrations), `scripts/seed/` (demo data) |
| Recorded PokeAPI payloads for tests | `src/test/resources/pokeapi` |
