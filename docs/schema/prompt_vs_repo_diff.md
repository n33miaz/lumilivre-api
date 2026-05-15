# Diff: Prompt (desenho pretendido) vs Repo (estado real)

> Divergencias entre o DDL descrito na fase de planejamento e o que o repositorio realmente tinha em `legacy/V1__baseline.sql` + entidades JPA. Documentado para justificar decisoes do baseline em ingles.
> Este documento reflete o estado no momento do freeze (fase zero do MIGRATION_PLAN.md).

## 1. Colunas que existiam no prompt e nao no repo

| Tabela | Coluna | Situacao |
|---|---|---|
| `aluno` | `texto_busca` (campo derivado/indexado) | Nao existe em V1. Nenhuma entidade referencia. |
| `emprestimo` | `aluno_nome_copia` (desnormalizacao para relatorio) | Nao existe em V1. Nenhuma entidade referencia. |

Decisao V2: **nao reintroduzir**. Substituido por:
- busca: indice GIN com `to_tsvector('portuguese', unaccent(full_name))` + `pg_trgm` (ADR-003).
- relatorio: view materializada ou JOIN direto com `student.full_name`; nao carrega copia desnormalizada na tabela de `loan`.

## 2. UNIQUE declarados diferentemente

| Campo | Prompt | V1 do repo | Decisao V2 |
|---|---|---|---|
| `usuario.email` | `UNIQUE NOT NULL` | sem `UNIQUE` | `UNIQUE NOT NULL` via `CITEXT` |
| `usuario.aluno_matricula` | `UNIQUE` | sem `UNIQUE` | `UNIQUE` (um aluno -> um `app_user`) |
| `token_reset_senha.usuario_id` | `UNIQUE` | sem `UNIQUE` | `UNIQUE` (um token ativo por usuario) |
| `aluno.email` | `UNIQUE` opcional | sem `UNIQUE` | `UNIQUE` parcial `WHERE email IS NOT NULL` |
| `aluno.cpf` | `UNIQUE` opcional | sem `UNIQUE` | `UNIQUE` parcial `WHERE cpf IS NOT NULL` |
| `aluno.matricula` | PK | PK (`VARCHAR(5)`) | vira coluna `UNIQUE NOT NULL` (`registration_number`); PK passa a `id UUID` |
| `livro.isbn` | `UNIQUE` opcional | `UNIQUE` | `UNIQUE` parcial `WHERE isbn IS NOT NULL` |
| `exemplar.tombo` | PK | PK `VARCHAR(10)` | vira coluna `UNIQUE NOT NULL` (`copy_code`); PK passa a `id UUID` |

## 3. Nullability divergente

| Tabela.coluna | Prompt | V1 do repo | Entidade JPA | Decisao V2 |
|---|---|---|---|---|
| `tcc.curso_id` | opcional (`NULL`) | `NOT NULL` | `NOT NULL` | `NOT NULL` (alinha com entidade e com regra de negocio) |
| `aluno.email` | opcional | opcional | opcional | opcional mas `UNIQUE` parcial |
| `aluno.cpf` | opcional | opcional | opcional | opcional mas `UNIQUE` parcial |
| `exemplar.status_livro` | `NOT NULL` | `NULL` permitido | — | `NOT NULL DEFAULT 'AVAILABLE'` |
| `emprestimo.status_emprestimo` | `NOT NULL` | `NULL` permitido | — | `NOT NULL DEFAULT 'ACTIVE'` |

## 4. Tipos divergentes ou implicitos

| Tabela.coluna | V1 | Decisao V2 | Motivo |
|---|---|---|---|
| `aluno.penalidade_expira_em` | `TIMESTAMP` | `TIMESTAMPTZ` | timezone explicito |
| `emprestimo.data_emprestimo` | `TIMESTAMP` | `TIMESTAMPTZ` | timezone explicito |
| `emprestimo.data_devolucao` | `TIMESTAMP` | `TIMESTAMPTZ` | timezone + separar `due_at` de `returned_at` |
| `aluno.email` | `VARCHAR(100)` | `CITEXT` | busca case-insensitive performante |
| `usuario.email` | `VARCHAR(255)` | `CITEXT` | idem |
| `aluno.cep` | `VARCHAR(8)` | `VARCHAR(8)` | mantido (padrao brasileiro) |
| `aluno.uf` | `VARCHAR(2)` | `CHAR(2)` | largura fixa |
| PKs de negocio | `SERIAL`/`BIGSERIAL`/`VARCHAR` | `UUID` | ADR-001 |

## 5. Colunas derivadas sem governanca

| Coluna | Origem | Decisao V2 |
|---|---|---|
| `aluno.emprestimos_count` | mantida em varios services | **remover** da tabela; calcular via view ou query (ADR-006) |
| `livro.quantidade` | mantida em ImportacaoService e LivroService | **remover**; calcular via `COUNT` de `book_copy` |

## 6. FKs sem `ON DELETE` explicito

Todas as FKs em V1 herdam o padrao Postgres (`NO ACTION`). Decisao V2 (plano secao 5.3.4):

- Referencias academicas (`course`, `academic_module`, `study_shift`, `genre`, `dewey_classification`): `ON DELETE RESTRICT`.
- `password_reset_token.app_user_id -> app_user`: `ON DELETE CASCADE`.
- `loan.student_id`, `loan.book_copy_id`, `loan_request.*`, `reservation.*`: `ON DELETE RESTRICT` (historico nao pode sumir).
- `book_genre.book_id`, `book_genre.genre_id`: `ON DELETE CASCADE` (associativa).

## 7. Indices ausentes no repo mas previstos no desenho alvo

Do plano (secao 5.5), nenhum existe hoje em V1-V3:

- `idx_student_full_name_trgm`
- `idx_student_course_module_shift`
- `idx_book_title_trgm`
- `idx_book_author_trgm`
- `idx_book_copy_book_id_status`
- `idx_loan_student_id_status_due_at`
- `idx_loan_book_copy_id_status`
- `idx_loan_status_due_at`
- `idx_loan_request_student_id_status_requested_at`
- `idx_reservation_book_id_status_queue_position`
- `idx_outbox_event_status_next_retry_at_created_at`

Parcialmente cobertos hoje por:
- `idx_emprestimo_status`, `idx_emprestimo_aluno`, `idx_emprestimo_exemplar` (equivalentes ingenuos)
- `idx_outbox_event_status_created_at`, `idx_outbox_event_status_retry_count`
- `idx_reserva_livro_status`

Todos serao recriados com nomenclatura EN e cobertura composta no PR 3.

## 8. Views materializadas

Existem (V4): `mv_dashboard_stats`, `mv_top_livros`, `mv_emprestimos_por_mes`.

Divergencias com desenho alvo:
- nomes em PT-BR -> EN (`mv_top_books`, `mv_loans_by_month`).
- `mv_top_livros` nao limita por janela temporal -> reavaliar se deve filtrar por ultimo ano para nao dominar ranking com titulos antigos.
- `mv_emprestimos_por_mes` usa `e.data_emprestimo` (data + hora); OK manter `DATE_TRUNC('month', ...)` mas ajustar tipo para `TIMESTAMPTZ`.

## 9. Constraints de negocio ausentes

Plano secao 5.6 pede unicidade parcial para reserva ativa. V3 nao tem:

```sql
CREATE UNIQUE INDEX uq_reservation_active_student_book
ON reservation (student_id, book_id)
WHERE status IN ('WAITING', 'READY');
```

## 10. Extensoes Postgres nao habilitadas

V1-V4 nao fazem `CREATE EXTENSION`. Em V2 serao habilitadas no baseline novo:

- `pgcrypto` - `gen_random_uuid()` para PKs
- `citext` - emails case-insensitive
- `pg_trgm` - ILIKE performatico com GIN
- `unaccent` - tsvector tolerante a acento

## 11. Extensoes nao instaladas mas planejadas

- `btree_gin` (opcional, plano secao 4.1): avaliar no PR 3 se indices compostos se beneficiam.

## 12. Resumo de impacto

- **Quebra estrutural**: PKs de todas as 8 tabelas de negocio mudam de tipo. Historico nao sobrevive por `dump-and-restore`; exige ETL se houver migracao de dados legados (nao e o caso - banco novo).
- **Quebra de contrato interno**: entidades JPA, repositories, queries nativas e materialized views tem nomes novos. DTOs publicos permanecem PT-BR (ADR-002).
- **Dados derivados removidos**: services precisam trocar leitura direta de contador por query agregada.
- **RLS**: tabelas expostas pelo Data API ganham politicas deny-by-default. Aplicacao Spring Boot segue acessando via JDBC + service_role e bypassa RLS.
