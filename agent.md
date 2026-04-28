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
- Public marketing pages are currently served from static files under:
  - `src/main/resources/static/public`
- The public routes `/`, `/about`, `/booking`, and `/confirmation` should continue to lead to the static public pages unless the project direction is explicitly changed.
- Do not silently re-enable Thymeleaf for the public pages during visual or content edits.

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
- For new public APIs, also prefer at least:
  - one controller test
  - one service test

## Current Files Relevant To This Policy

- `src/main/resources/application.properties`
- `src/main/resources/application-h2.properties`
- `src/main/resources/application-mysql.properties`
- `src/main/resources/db/migration/V1__create_store_holidays.sql`
- `src/main/resources/db/migration/V2__create_public_reservations.sql`
- `src/main/resources/mapper/StoreHolidayMapper.xml`
- `src/main/resources/mapper/PublicReservationMapper.xml`
- `src/main/java/com/Edo_perfume/ScentOfASA/holiday/service/StoreHolidayService.java`
- `src/main/java/com/Edo_perfume/ScentOfASA/reservation/service/PublicBookingService.java`
- `src/main/java/com/Edo_perfume/ScentOfASA/web/PublicPageController.java`

## Notes For Future Work

- The project is expected to grow to roughly 5 to 10 times the current volume.
- Because of that, short-term convenience choices that increase DB lock-in should be avoided.
- If a new feature requires DB-specific SQL, document the reason clearly in code or in a follow-up note.
- Some Japanese text may look garbled when read through the CLI or PowerShell in this environment.
- If VSCode shows the file normally, treat the file contents as the source of truth and assume the garbling may be a terminal-display issue rather than file corruption.

## Public Page Policy

- The current source of truth for public-facing page markup is:
  - `src/main/resources/static/public`
- Public pages should be edited there first unless the team explicitly decides to move them back to server-rendered templates.
- `src/main/resources/templates/public` may remain in the repository, but it should be treated as inactive for the current public site unless re-adopted intentionally.
- When restoring or editing public pages, verify that routing still matches the static files and does not accidentally render older Thymeleaf content.

## Public Booking Architecture

- The source of truth for booking business rules must stay on the Java side, not in frontend JavaScript.
- Holiday checks, slot availability checks, and final reservation acceptance must be decided in service-layer code.
- Frontend scripts such as `booking.js` should consume API responses and reflect state in the UI, but should not become the authoritative implementation of booking rules.
- Avoid duplicating holiday logic or slot-availability logic in both Java and JavaScript.

## Public Booking API Baseline

- Public booking availability is exposed through:
  - `GET /api/public/availability`
- Public reservation creation is exposed through:
  - `POST /api/public/reservations`
- These endpoints are the intended integration point for the public booking UI.
- The initial minimum booking assumptions are:
  - supported guide languages: `ja`, `en`
  - supported public time slots: `11:00`, `13:00`, `15:30`
  - one reservation record occupies one slot for one language
  - guest count is currently limited to `1` through `4`
- The reservation persistence baseline is:
  - `public_reservations` managed by Flyway
  - timestamps written from Java code
  - slot uniqueness enforced in DB and rechecked in service logic

## Holiday Control Rules

- In the holiday control screen:
  - `蜈ｨ莠育ｴЯ means shared + English-only + Japanese-only records are all shown
  - `闍ｱ隱樔ｺ育ｴЯ means shared + English-only records are shown
  - `譌･譛ｬ隱樔ｺ育ｴЯ means shared + Japanese-only records are shown
- Weekly closing rules applied from the holiday control screen must always be stored as shared (`applies_to_language = null`), not language-specific.
- Open exception dates for weekly closing rules are stored as `SPECIAL_OPEN`.
- When a date is changed to a shared / all-reservations holiday setting, language-specific records on the same date should be removed so stale `(譌･譛ｬ隱樔ｺ育ｴ・` or `(闍ｱ隱樔ｺ育ｴ・` labels do not remain.

## Maven Wrapper Notes

- `mvnw.cmd` had a wrapper-script bug around Maven user home resolution:
  - it accessed `Target[0]` directly even when `.m2` was a normal directory or had an empty `Target`
  - this caused `Cannot index into a null array` in PowerShell
- That null / empty-target handling has been patched in `mvnw.cmd`.
- If wrapper execution still fails in this environment, the next likely causes are environmental rather than project-code issues:
  - local repository path access under the default user home
  - blocked network access when downloading the Maven distribution zip from `distributionUrl`
- In constrained environments, it may be necessary to point `MAVEN_USER_HOME` to a writable directory inside the workspace before running `mvnw.cmd`.
- If `mvnw.cmd` fails with remote-download errors, treat that as an environment or network limitation before assuming the project build itself is broken.
