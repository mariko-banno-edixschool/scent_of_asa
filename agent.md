# SCENT OF ASA Agent Notes

## Current Project State

- This project is a Spring Boot 3.5 + MyBatis application with many static HTML pages already prepared.
- Backend implementation is still partial. The first implemented business feature is `store_holidays`.
- The repository currently uses:
  - `Spring Boot`
  - `MyBatis`
  - `Flyway`
  - `H2` for school-PC development
  - `MySQL` for main / target environment

## DB Strategy

- `MySQL` is the primary database design target.
- `H2` is the default local development database because MySQL cannot be used on the school PC.
- Profiles are split as:
  - `application.properties`: shared settings
  - `application-h2.properties`: default development profile
  - `application-mysql.properties`: MySQL profile

## How To Run

- Default startup uses `h2`.
- To run with MySQL, use `--spring.profiles.active=mysql`.

## Migration Policy

- Do not add new tables through ad hoc `schema.sql` or `spring.sql.init.*`.
- Add schema changes through Flyway migrations under:
  - `src/main/resources/db/migration`
- Existing `store_holidays` schema is managed by:
  - `V1__create_store_holidays.sql`

## SQL / DB Compatibility Rules

- Avoid MySQL-only SQL as much as possible.
- Be especially careful with:
  - `AUTO_INCREMENT`
  - `DATETIME`
  - `ON UPDATE CURRENT_TIMESTAMP`
  - MySQL-specific functions and expressions such as `DATE_FORMAT`, `IFNULL`, `ENUM`, JSON-specific features, and DB-specific syntax in complex queries

## Decisions Already Made

- `DATETIME`-style automatic DB handling was reduced by storing timestamps from Java.
- `ON UPDATE CURRENT_TIMESTAMP` is no longer relied on for `updated_at`.
- `created_at` and `updated_at` are set in application code instead of depending on DB auto-update behavior.
- `store_holidays` currently works with H2 in MySQL mode and passes tests.

## Implementation Guidance For New Tables

- When adding a new table:
  - create a Flyway migration first
  - keep schema as cross-DB-friendly as possible
  - avoid pushing timestamp update rules into DB-specific features
  - keep MyBatis SQL simple and portable
  - add at least minimum mapper and service tests

## Testing Policy

- Keep `mvn test` runnable on the school PC.
- Prefer:
  - mapper tests with H2
  - service tests with mocks where appropriate
- New DB-backed features should come with at least:
  - one mapper test
  - one service test

## Current Files Relevant To This Policy

- `src/main/resources/application.properties`
- `src/main/resources/application-h2.properties`
- `src/main/resources/application-mysql.properties`
- `src/main/resources/db/migration/V1__create_store_holidays.sql`
- `src/main/resources/mapper/StoreHolidayMapper.xml`
- `src/main/java/com/Edo_perfume/ScentOfASA/holiday/service/StoreHolidayService.java`

## Notes For Future Work

- The project is expected to grow to roughly 5 to 10 times the current volume.
- Because of that, short-term convenience choices that increase DB lock-in should be avoided.
- If a new feature requires DB-specific SQL, document the reason clearly in code or in a follow-up note.
