# ADR-013 — PostgreSQL como dependência forte do LumiLivre

| Campo       | Valor                                                                 |
|-------------|-----------------------------------------------------------------------|
| Status      | Accepted                                                              |
| Data        | 2026-05-22                                                            |
| Decisor     | Time backend LumiLivre                                                |
| Contexto    | Fase de preparação para release `0.1.0` open-source                   |
| Substitui   | —                                                                     |
| Substituido por | —                                                                |

## Contexto

A LumiLivre API depende hoje de **vários recursos específicos do
PostgreSQL** que não existem (ou exigem refactor significativo) em
outros SGBDs relacionais:

- `pg_trgm` (GIN) para busca por similaridade em livros e alunos.
- `unaccent` + função `IMMUTABLE` para queries com acentos transparentes.
- `CITEXT` para e-mails case-insensitive sem `LOWER()` espalhado.
- `MATERIALIZED VIEW` + `REFRESH MATERIALIZED VIEW CONCURRENTLY` para o
  dashboard administrativo.
- `UNIQUE INDEX ... WHERE deleted_at IS NULL` (índice parcial) para
  permitir soft-delete sem perder a unicidade de chaves de negócio
  (`book.isbn`, `student.email`, `student.cpf`).
- `gen_random_uuid()` via `pgcrypto`.
- `Row Level Security` (deny-by-default) — central para o caso de uso
  Supabase, onde a Data API expõe o banco diretamente.

Durante a fase de fechamento do release, surgiu a questão: **devemos
manter Postgres como dependência forte ou perseguir portabilidade**?

## Forças

| Pró Postgres forte                                  | Contra Postgres forte                                   |
|-----------------------------------------------------|---------------------------------------------------------|
| Schema mais limpo (CITEXT, partial UNIQUE).         | Bloqueia adoção em ambientes "só MySQL/MariaDB".        |
| Dashboard < 500 ms graças às MVs.                   | Engenheiros sem familiaridade pagam custo de aprendizado.|
| Busca trigram performa em qualquer tamanho de base. | Perde-se a opção de embarcar em SQLite para demo.       |
| RLS habilita modelo deny-by-default Supabase.       | Render/Supabase compartilham fornecedor.                |
| Suporte Java/JPA é maduro (driver 42.7+).           |                                                         |
| Comunidade FOSS forte (sem lock-in de fornecedor).  |                                                         |

## Decisão

Manter **PostgreSQL** como dependência forte do LumiLivre por pelo menos
o ciclo `0.x`. Especificamente:

1. O baseline V1..V5 continua usando recursos específicos do Postgres.
2. Documentaremos explicitamente como portar para outros bancos (este
   ADR + `portability_notes.md`).
3. **Não** vamos manter uma "compatibility layer" abstraindo o SQL no
   código — repositórios podem usar SQL puro Postgres quando isso
   simplifica.
4. Para uso autocontido sem Supabase, oferecemos:
   - `docs/database/ddl_standalone.sql` — DDL pronto para qualquer
     Postgres 13+ self-hosted.
   - `docker-compose.yml` (em F1 do plano FINISH) com Postgres local.

## Consequências

### Positivas

- Schema permanece compacto. `application.properties` continua simples.
- Dashboard mantém performance (MVs).
- Busca textual continua trabalhando bem em PT-BR sem hacks.
- Modelo RLS pronto para qualquer cliente PostgREST/Supabase.

### Negativas

- Empresas que padronizaram em MySQL/MariaDB precisam refatorar para
  adotar (custo documentado em `portability_notes.md`).
- Versão Postgres mínima é 13 (por causa de `gen_random_uuid()` via
  `pgcrypto`); Postgres 11 e anteriores não funcionam sem mudanças.

### Neutras

- Continuamos suportando **tanto** Supabase managed quanto Postgres
  self-hosted. O backend usa o driver oficial; o que muda é apenas a
  URL de conexão e a presença/ausência de RLS forçada.

## Caminhos não escolhidos

### Opção rejeitada: abstrair via ORM agnóstico

Considerado: usar **somente JPA/JPQL** evitando SQL nativo e features
específicas Postgres. Rejeitado porque:

- Perderíamos as MVs do dashboard (JPA não as cobre nativamente).
- Busca trigram não tem equivalente JPA.
- Migrations Flyway perderiam capacidade de criar índices GIN.
- Ganho marginal: ainda assim precisaríamos de driver-específico para
  comportamentos como `CITEXT`.

### Opção rejeitada: dual-stack Postgres + MariaDB

Considerado: manter Flyway com dois conjuntos de migrations
(`db/migration/postgres/` e `db/migration/mariadb/`). Rejeitado porque:

- Duplicação de SQL → divergências inevitáveis.
- Custo de testes 2x (Testcontainers para ambos).
- Dashboard ficaria intrinsicamente diferente entre os dois deploys.

## Revisão

Reavaliar este ADR quando:

- Um adotante real demandar MySQL/MariaDB com volume de uso > 10
  bibliotecas/instâncias.
- Postgres deixar de oferecer suporte LTS para a versão usada
  (atualmente 16).
- O custo total de manutenção da dependência forte ultrapassar 1 dia/mês
  de engenharia (estamos hoje < 1 dia/trimestre).

## Referências

- `docs/database/ERD.md` — modelo lógico.
- `docs/database/data_dictionary.md` — catálogo detalhado.
- `docs/database/portability_notes.md` — guia de migração entre bancos.
- `docs/database/migration_from_legacy.md` — guia de importação a
  partir de Pergamum, Biblivre etc.
- ADR-004 — escolha do Supabase Session pooler.
- ADR-005 — habilitação de RLS deny-by-default.
