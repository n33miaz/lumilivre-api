# Contributing to API docs

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
