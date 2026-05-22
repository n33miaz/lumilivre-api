# Changelog

All notable changes to **LumiLivre API** are documented here.

The format is based on [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

Dates use `YYYY-MM-DD`. Unreleased changes accumulate at the top until
a new tag is cut.

---

## [Unreleased]

### Added
- Database documentation pack under `docs/database/`: ER diagram, data
  dictionary, standalone DDL, migration-from-legacy guide, portability
  notes and ADR-013 recording PostgreSQL as a strong dependency.
- Demo seed (`db/seed/R__seed_demo_data.sql`) now exercises every UI
  state: 30 books (10+ genres, public OpenLibrary covers, volume=2),
  8 students with two penalty profiles, 15 book copies covering
  AVAILABLE/BORROWED/OVERDUE/MAINTENANCE/UNAVAILABLE, 10 loans, 6 loan
  requests, 5 reservations across the full FIFO lifecycle, 3 theses
  with external `pdf_url`, audit_log entries and sent outbox events.
- i18n keys for service-emitted emails: `email.loan-{created,completed,
  renewed}.{subject,body}`, `email.reservation-ready.{subject,body}`,
  `email.penalty.none`, `request.email.{received,accepted,rejected}.
  {subject,body}`, `user.role.{admin,librarian,student}`,
  `book.{create,update}.failed`, `book.cover.upload-failed`.
- `app.email.from` and `app.public-url` configuration properties to
  externalise the outbound `From:` header and the marketing URL used
  in invitation emails.

### Changed
- `LoanService`, `LoanRequestService`, `AppUserService` resolve email
  subjects/bodies through `MessageResolver` using each user's
  `app_user.preferred_locale` instead of hardcoded PT-BR strings.
- `BusinessMetricsService` Prometheus gauge descriptions switched to
  English (aligns with metric naming convention).
- `BookService` no longer throws bare `RuntimeException` for internal
  failures — surfaces `BusinessRuleException.ofKey(...)` so the
  `GlobalExceptionHandler` can render a localised 422 response.

### Fixed
- `EmailService` outbound `From:` corrected: `contato.lumlivre@...` →
  `contato.lumilivre@gmail.com` (typo fix in hostname).
- `DashboardServiceTest`: aligned expectation with the actual
  non-concurrent refresh of `mv_dashboard_stats`. The materialized view
  uses a `UNIQUE INDEX ((1))` which only permits non-concurrent refresh.

### Documentation
- README references to `/swagger-ui.html` updated to follow the new
  `/docs` mountpoint (coordinated with the parallel Swagger overhaul).

---

## [0.1.0-rc1] - 2026-05-22

> Pre-release tag. Not published to any registry yet.

### Added
- Baseline V1..V5 (English schema) covering 17 tables, materialized
  views for the admin dashboard, GIN trigram indexes for catalogue
  search and RLS deny-by-default policies for Supabase deployments.
- Outbox pattern (`outbox_event` + `OutboxPublisherService`) decoupling
  SMTP from request transactions; at-least-once delivery with retry
  cap of 3.
- Audit trail (`audit_log` + `@Auditable`) for admin actions covering
  loans, students, books, theses and user lifecycle.
- Reservation queue with FIFO + READY/EXPIRED state machine and email
  notification when a copy becomes available.
- Daily scheduled job marking ACTIVE loans as OVERDUE and sending the
  D-3 / D-1 / D0 / overdue notifications.
- Resilience4j circuit breaker + retry + timeout + fallback wrapping
  Google Books, BrasilAPI and Supabase Storage.
- Bucket4j rate-limit (5 req / 10 min) on `/auth/login` and
  `/auth/esqueci-senha`.
- i18n infrastructure: `Accept-Language` resolver, `MessageResolver`
  bean and per-locale bundles for auth/book/course/dashboard/email/
  enum/student/thesis/user/validation/request/reservation/loan
  surfaces.
- Springdoc OpenAPI bundle with two language-specific groups
  (`api-pt-br`, `api-en-us`).
- Materialized view refresh job (`DashboardService.refreshViews`) every
  15 minutes when the JDBC connection is PostgreSQL.
- Optional demo seed (`db/seed/R__seed_demo_data.sql`) gated by
  `LUMILIVRE_FLYWAY_LOCATIONS`.
- CI workflow (`.github/workflows/api.yml`) running Gitleaks secrets
  scan, `./mvnw verify` (Surefire + JaCoCo) and packaging into the
  release jar artefact.
- Dependabot configuration for Maven and GitHub Actions
  dependencies.

### Security
- All `LUMILIVRE_*` placeholders required at boot; the application
  refuses to start without them, preventing silent fallback to insecure
  defaults.
- `SecurityConfig` allowlist explicit; `anyRequest().authenticated()`
  closes the default-deny policy. `@CanAccessStudent` enforces IDOR
  protection between students.

---

[Unreleased]: https://github.com/n33miaz/lumilivre-api/compare/v0.1.0-rc1...HEAD
[0.1.0-rc1]: https://github.com/n33miaz/lumilivre-api/releases/tag/v0.1.0-rc1
