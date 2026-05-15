# ADR 003 - Estrategia de busca textual

- **Status**: aceito
- **Data**: 2026-04-23
- **Contexto**: fase zero da migracao V2
- **Relacionado**: MIGRATION_PLAN.md secoes 4.1, 5.4 e 5.5

## Contexto

O sistema atual executa busca por nome de aluno, titulo e autor de livro via `ILIKE`/`LOWER` + `LIKE` sem indice apropriado. A versao em portugues do prompt sugeria uma coluna `texto_busca` materializada manualmente.

Problemas:

- `ILIKE '%termo%'` sem indice faz sequencial scan em toda a tabela.
- `LOWER(col) LIKE LOWER(?)` nao usa indice B-tree comum; precisa de expression index.
- Acentuacao nao e tratada: busca por "maria" nao encontra "María".
- Materializar `texto_busca` exige trigger ou atualizacao manual em todo insert/update.

## Decisao

Nao materializar coluna `texto_busca`. Substituir por dois tipos de indice expressao:

### 1. `pg_trgm` + GIN para busca por substring (ILIKE)

Cobertura:

- `student.full_name`
- `book.title`
- `book.author`

Indice exemplo:

```sql
CREATE INDEX idx_student_full_name_trgm
  ON student USING gin (unaccent(full_name) gin_trgm_ops);
```

Busca na aplicacao:

```sql
SELECT * FROM student
 WHERE unaccent(full_name) ILIKE unaccent(?) || '%'
    OR unaccent(full_name) ILIKE '%' || unaccent(?) || '%'
 ORDER BY full_name;
```

### 2. `tsvector` GIN expressao para busca full-text

Cobertura: `book.title` + `book.author` + `book.synopsis` (catalogo completo).

Indice expressao:

```sql
CREATE INDEX idx_book_fts
  ON book USING gin (
    to_tsvector('portuguese',
      unaccent(coalesce(title, '') || ' ' ||
               coalesce(author, '') || ' ' ||
               coalesce(synopsis, '')))
  );
```

Busca:

```sql
SELECT * FROM book
 WHERE to_tsvector('portuguese', unaccent(title || ' ' || author || ' ' || coalesce(synopsis, '')))
       @@ plainto_tsquery('portuguese', unaccent(?));
```

### Extensoes habilitadas no baseline V2

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
```

`unaccent` entra como funcao; se houver preocupacao com `IMMUTABLE` em indice expressao, encapsular em uma funcao wrapper marcada como `IMMUTABLE`:

```sql
CREATE OR REPLACE FUNCTION immutable_unaccent(text) RETURNS text AS $$
  SELECT unaccent('unaccent', $1);
$$ LANGUAGE sql IMMUTABLE;
```

## Consequencias

### Positivas

- Buscas indexadas e aceleradas para os campos de fato usados (catalogo e ranking).
- Sem manutencao de coluna materializada ou triggers.
- Tolerancia a acento out of the box.
- Rankeamento opcional via `ts_rank` quando for utilizado.

### Negativas

- Indices GIN sao mais pesados que B-tree; insercao em massa paga custo extra.
- Consultas devem ser revisadas: a forma `ILIKE unaccent(?)` nao e automatica, precisa ser explicita no repository.
- Depende de `pg_trgm` e `unaccent` disponiveis no Supabase (confirmado).

### Operacionais

- Reindex se `catalog/unaccent` mudar (raro).
- Monitorar tamanho dos indices GIN em `pg_stat_user_indexes`.

## Alternativas consideradas

- **Coluna `texto_busca` materializada + trigger**: rejeitado. Complexidade de atualizacao; trigger vira ponto de erro em imports em lote.
- **Elasticsearch/Meilisearch/Typesense**: rejeitado nesta fase. Traz nova dependencia e nao resolve nada que GIN + trigram nao resolva no volume atual.
- **B-tree com `LOWER()`**: rejeitado. Nao ajuda em `ILIKE '%x%'`; so cobre prefixo.
- **Manter ILIKE sem indice**: rejeitado. Ja e gargalo confirmado em lista de alunos e catalogo mobile.

## Referencias

- MIGRATION_PLAN.md secoes 4.1, 5.4, 5.5
- SCHEMA_DICTIONARY.md secao 7 (nomes de indices alvo)
- Postgres docs: pg_trgm, unaccent, tsvector
