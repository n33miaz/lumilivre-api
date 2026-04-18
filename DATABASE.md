# Database Migrations — LumiLivre API

## Ferramenta

**Flyway** — migrações versionadas localizadas em `src/main/resources/db/migration/`.

## Convenção de Nomes

```
V<sequência>__<descrição_snake_case>.sql
```

Exemplos:
- `V1__baseline.sql` — schema completo do banco existente (gerado uma única vez)
- `V2__add_reserva_table.sql`
- `V3__add_audit_log.sql`

## Configuração

| Propriedade | Valor |
|---|---|
| `spring.flyway.baseline-on-migrate` | `true` (banco existente) |
| `spring.flyway.baseline-version` | `0` |
| `spring.flyway.locations` | `classpath:db/migration` |

## Como rodar via Maven CLI

```bash
export LUMILIVRE_DB_URL=jdbc:postgresql://<host>:5432/postgres
export LUMILIVRE_DB_USER=postgres.<ref>
export LUMILIVRE_DB_PASSWORD=<senha>

./mvnw flyway:migrate
./mvnw flyway:info
./mvnw flyway:validate
```

## Regras

1. **Nunca editar** uma migration já aplicada em produção.
2. Para corrigir schema, criar uma nova migration (`V<N+1>__fix_...sql`).
3. O arquivo `V1__baseline.sql` deve ser gerado por `pg_dump --schema-only` do Supabase.
4. Cada PR com alteração de schema deve incluir a migration correspondente.
