# Adding another Swagger language

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
