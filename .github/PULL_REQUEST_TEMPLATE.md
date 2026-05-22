<!--
Thanks for contributing to LumiLivre API.
Keep the title under 70 characters, follow Conventional Commits.
-->

## Summary

<!-- 1-3 bullets describing what changes and why. -->

-
-

## Type of change

<!-- Mark with an `x` all that apply. -->

- [ ] `fix` — bug fix (no breaking change)
- [ ] `feat` — new feature (no breaking change)
- [ ] `refactor` — internal change without behaviour shift
- [ ] `docs` — documentation only
- [ ] `chore` / `build` / `ci` — tooling
- [ ] **Breaking change** (describe migration path below)

## Test plan

<!--
Bulleted checklist of what was tested. Required for `feat`/`fix`/`refactor`.
Example:
- [ ] `./mvnw verify` green locally
- [ ] Smoke test: POST /api/loans returns 201 with valid payload
- [ ] Manual UI walk-through on /admin/loans
-->

- [ ]
- [ ]

## Related issues

<!-- Use `Closes #123` for issues this PR resolves; `Refs #456` for related ones. -->

## Database changes

- [ ] No schema change
- [ ] Adds Flyway migration `Vxx__*.sql`
- [ ] Adds repeatable migration `R__*.sql`
- [ ] Modifies `db/seed/`

## i18n

- [ ] No user-visible strings added
- [ ] All new strings sourced via `MessageResolver` and present in both
      `messages_pt_BR.properties` and `messages_en_US.properties`
- [ ] `bash scripts/check-i18n-coverage.sh` green

## Security

- [ ] No secret was added to the diff (`.env`, JWT, API key, certificate)
- [ ] No new public endpoint without `@PreAuthorize` or explicit allowlist entry
- [ ] If touching `SecurityConfig`, blast radius is described above

## Screenshots / API examples

<!-- Optional for `feat`/`fix`. Required for endpoints that change response shape. -->

---

<!--
Reviewer checklist (do not delete):
- [ ] Coverage gate (`jacoco.enforce`) status appropriate
- [ ] OpenAPI examples updated when contract changed
- [ ] CHANGELOG.md `[Unreleased]` entry added
-->
