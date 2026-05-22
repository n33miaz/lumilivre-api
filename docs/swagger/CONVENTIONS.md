# Swagger/OpenAPI conventions

This document is the source of truth for LumiLivre API documentation.

## Principles

- Controllers declare stable machine identifiers only: `@Tag(name = SwaggerTags.BOOKS)` and `@Operation(operationId = "books.list")`.
- Human-facing text lives in `src/main/resources/i18n/swagger/*_{pt_BR,en_US}.properties`.
- The same operation must render correctly in `api-pt-br` and `api-en-us`.
- Public endpoints must not show the bearer lock. Protected endpoints must show bearer authentication.
- New tags must be added to `SwaggerTags.ORDERED` and to both localized bundles.

## Operation IDs

Use `{tagKey}.{verb}`:

| HTTP/use case | Suffix examples |
|---|---|
| list/search | `list`, `search`, `advanced`, `byStudent` |
| read one | `get`, `isbnLookup`, `postalCode` |
| create/update/delete | `create`, `update`, `delete` |
| actions | `close`, `renew`, `cancel`, `process` |
| uploads/imports | `uploadCover`, `uploadAvatar`, `students`, `books`, `copies` |

Examples: `auth.login`, `books.catalog`, `loans.close`, `loan-requests.process`.

## Bundle keys

Minimum keys per operation:

```properties
swagger.operation.{operationId}.summary=Short action label
swagger.operation.{operationId}.description=Business purpose, roles and relevant rules.
```

Optional keys:

```properties
swagger.parameter.{operationId}.{paramName}.description=Specific parameter wording.
swagger.requestBody.{operationId}.description=Request body purpose.
swagger.response.{operationId}.{status}.description=Specific response wording.
swagger.schema.{SchemaName}.description=Schema purpose.
swagger.schema.{SchemaName}.{field}.description=Field purpose.
```

Shared fallback keys live in `_common_{locale}.properties`.

## CI checks

Run before committing:

```bash
bash scripts/check-i18n-coverage.sh
bash scripts/check-openapi-annotations.sh
./mvnw test
```
