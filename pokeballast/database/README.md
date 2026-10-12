# Database

| Folder | Content |
|---|---|
| [`model/`](model/) | Data model design: entity-relationship diagram, tables, columns, constraints, indexes and the reasons behind them. |
| [`scripts/`](scripts/) | SQL scripts. They are the **Flyway migrations** applied at startup: Maven packages this folder into the classpath at `db/migration`, which is where Flyway reads from. There is a single copy of each script. |
| [`scripts/seed/`](scripts/seed/) | Demo data (`R__seed_demo_data.sql`), packaged separately at `db/seed`. The app loads it; the automated tests do not (only `SeedDataTest` does, to prove it applies). |

## Script rules

- Naming: `V<n>__<description>.sql` for schema changes, for example `V2__create_app_user.sql`. Numbers are consecutive and never reused.
- Seed: a **repeatable** script (`R__...`). Every insert is idempotent (`insert ... select ... where not exists`), because Flyway re-runs it whenever the file changes.
- **Never edit a script that has already been applied.** Any change, even a fix, is a new script. Flyway validates checksums and refuses to start if an applied script changed.
- Use portable SQL: the same scripts run on H2 in PostgreSQL mode (local runs and tests) and on PostgreSQL.
- Every script that changes the schema also updates `model/`.
- Hibernate never creates or alters tables (`ddl-auto: validate`). It only checks that the JPA mapping matches the schema.
