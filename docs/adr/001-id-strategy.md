# ADR 001 - Estrategia de identificadores

- **Status**: aceito
- **Data**: 2026-04-23
- **Contexto**: fase zero da migracao V2 (banco Supabase novo, schema em ingles)
- **Relacionado**: MIGRATION_PLAN.md secao 3.2, SCHEMA_DICTIONARY.md

## Contexto

O schema legacy usa tres tipos diferentes de PK para tabelas de negocio:

- `aluno.matricula VARCHAR(5)` (chave natural externa)
- `livro.id BIGSERIAL`
- `usuario.id SERIAL`
- `exemplar.tombo VARCHAR(10)` (chave natural externa)
- `emprestimo.id SERIAL`

Efeitos observados:

1. IDs sequenciais sao enumeraveis por cliente externo (Web/App), facilitando ataques de enumeracao e revelando ordem e volume.
2. Chaves naturais (matricula, tombo) misturam identidade tecnica com chave de negocio: renomear o codigo do aluno ou do tombo se torna uma migracao de grande porte.
3. Inconsistencia dificulta abstracoes (services, DTOs, mapeadores).
4. Geracao via `BIGSERIAL` exige round-trip com o banco para obter o ID apos insert, o que complica write-then-publish para outbox/eventos.

## Decisao

Adotar tres classes de identificador conforme a natureza da tabela.

### UUID v4 (`gen_random_uuid()` via `pgcrypto`) para tabelas de negocio

- `student`
- `app_user`
- `book`
- `book_copy`
- `loan`
- `loan_request`
- `reservation`
- `thesis`

Chaves naturais que antes eram PK viram colunas `UNIQUE NOT NULL`:

- `student.registration_number VARCHAR(10) UNIQUE NOT NULL`
- `book_copy.copy_code VARCHAR(20) UNIQUE NOT NULL`

Geracao no banco (`DEFAULT gen_random_uuid()`), com Hibernate mapeando como `UUID`.

### INTEGER / SMALLINT para tabelas de referencia pequenas

- `course`, `academic_module`, `study_shift`, `genre`, `dewey_classification`

Motivo: volume baixo, sem exposicao externa, sem ganho em ofuscar IDs.

### BIGINT `GENERATED ALWAYS AS IDENTITY` para tabelas de infraestrutura append-only

- `audit_log`
- `outbox_event`
- `password_reset_token`

Motivo: insercao massiva sequencial, ordenacao temporal natural, sem exposicao externa direta.

## Consequencias

### Positivas

- IDs de negocio imprevisiveis; reduz superficie de enumeracao.
- Geracao client-side possivel (aplicacao pode gerar UUID antes do insert, util para outbox transacional).
- Identidade tecnica desacoplada das chaves de negocio. Renomear uma matricula nao exige atualizar FKs.
- Consistencia em toda a camada de negocio facilita generics (`Repository<T, UUID>`).

### Negativas

- UUIDs ocupam 16 bytes (vs 4 do INT). Indices ficam maiores; tabelas com volume muito alto (outbox, audit) evitam o custo.
- Range scans por ID nao tem significado cronologico. Quando o caso de uso exige ordenacao temporal, usar `created_at` ou `id BIGINT` (audit/outbox).
- Importacao a partir de planilhas precisa gerar UUIDs ou permitir lookup por chave natural (`registration_number`, `copy_code`).

### Migracao de codigo

- Repositorios e services passam a trabalhar com `UUID` em vez de `Long`/`String` para essas entidades.
- DTOs publicos permanecem com o formato atual (matricula/tombo como string) conforme ADR-002; o mapeamento acontece na camada de controller.
- Endpoints que hoje recebem `/alunos/{matricula}` continuam funcionais: o repository ganha um `findByRegistrationNumber` e o controller traduz.

## Alternativas consideradas

- **UUID em toda tabela**: rejeitado. Tabelas de referencia com 5-20 linhas nao ganham nada e perdem legibilidade em debug.
- **ULID / KSUID**: rejeitado. Postgres nao tem suporte nativo; traz dependencia extra sem ganho critico sobre UUIDv4 + `created_at` indexado.
- **Manter SERIAL em tudo**: rejeitado. Mantem o problema de enumeracao publica e mistura identidade com chave natural.

## Referencias

- MIGRATION_PLAN.md secao 3.2
- `docs/schema/SCHEMA_DICTIONARY.md` secao 2 e 4
