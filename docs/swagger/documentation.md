# swagger/openapi docs

## conventions

This document is the source of truth for LumiLivre API documentation.

### principles

- Controllers declare stable machine identifiers only: `@Tag(name = SwaggerTags.BOOKS)` and `@Operation(operationId = "books.list")`.
- Human-facing text lives in `src/main/resources/i18n/swagger/*_{pt_BR,en_US}.properties`.
- The same operation must render correctly in `api-pt-br` and `api-en-us`.
- Public endpoints must not show the bearer lock. Protected endpoints must show bearer authentication.
- New tags must be added to `SwaggerTags.ORDERED` and to both localized bundles.

### operation ids

Use `{tagKey}.{verb}`:

| HTTP/use case | Suffix examples |
|---|---|
| list/search | `list`, `search`, `advanced`, `byStudent` |
| read one | `get`, `isbnLookup`, `postalCode` |
| create/update/delete | `create`, `update`, `delete` |
| actions | `close`, `renew`, `cancel`, `process` |
| uploads/imports | `uploadCover`, `uploadAvatar`, `students`, `books`, `copies` |

Examples: `auth.login`, `books.catalog`, `loans.close`, `loan-requests.process`.

### bundle keys

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

## contributing

When adding or changing an endpoint:

1. Add or update `@Tag(name = SwaggerTags.X)` on the controller.
2. Add `@Operation(operationId = "{tag}.{verb}")` on every mapping method.
3. Add `swagger.operation.{operationId}.summary` and `.description` in PT-BR.
4. Add the exact same keys in EN-US.
5. Add request-body and parameter keys when the generated wording is not enough.
6. Run:

```bash
bash scripts/check-i18n-coverage.sh
bash scripts/check-openapi-annotations.sh
./mvnw test
```

Do not put user-facing text directly inside annotations. The Java source should remain stable when only wording or translations change.

## adding another swagger language

Example for Spanish (`es-ES`):

1. Copy each `src/main/resources/i18n/swagger/*_pt_BR.properties` file to `*_es_ES.properties`.
2. Translate values only. Keep keys unchanged.
3. Add the locale to `I18nConfig.localeResolver().setSupportedLocales(...)`.
4. Add a new group in `OpenApiConfig`:

```java
@Bean
public GroupedOpenApi apiEsEsGroup() {
    return localizedGroup("api-es-es", "API - Espanol (Espana)", Locale.forLanguageTag("es-ES"));
}
```

5. Run the coverage script and the tests.

Adding a language should not require changing controllers or operation IDs.

## glossary

| PT-BR | EN-US | Notes |
|---|---|---|
| Empréstimo | Loan | Physical checkout of a copy by a student. |
| Exemplar | Book copy | Physical inventory unit identified by copy code/tombo. |
| Aluno | Student | Library user with a registration number. |
| Bibliotecário | Librarian | Operational staff role. |
| Usuário administrativo | Admin user | ADMIN or LIBRARIAN account. |
| Solicitação | Loan request | Request that may become a loan when accepted. |
| Reserva | Reservation | FIFO queue for a book. |
| TCC | Thesis | Final-year academic work. |
| CDD | Dewey Classification | Keep `CDD` in Portuguese contexts. |
| Tombo | Copy code | Inventory identifier of a physical copy. |
| Matrícula | Registration number | Student identifier. |
| CEP | Postal code | Keep `CEP` only in PT-BR texts. |
