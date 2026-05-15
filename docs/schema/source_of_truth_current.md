# Source of Truth - Schema Atual (legacy)

> Snapshot do schema PT-BR que existia no repositorio em `src/main/resources/db/migration/legacy/` no momento do freeze para a migracao V2.
> Este documento descreve **o que era**. A partir daqui nenhum codigo nasce em PT-BR.

## 1. Origem

Arquivos analisados:

- `src/main/resources/db/migration/legacy/V1__baseline.sql` - estrutura base (16 tabelas)
- `src/main/resources/db/migration/legacy/V2__outbox_event.sql` - outbox de emails
- `src/main/resources/db/migration/legacy/V3__audit_log_and_reserva.sql` - audit + reserva + coluna `renovacoes`
- `src/main/resources/db/migration/legacy/V4__materialized_views.sql` - views do dashboard

Entidades JPA cruzadas: `src/main/java/br/com/lumilivre/api/model/*Model.java`.

## 2. Tabelas e caracteristicas principais

### 2.1 Tabelas de referencia (dominio estatico)

| Tabela | PK | Campos | Observacao |
|---|---|---|---|
| `cdd_classificacao` | `codigo VARCHAR(255)` | `descricao` | Codigo Dewey textual |
| `curso` | `id INTEGER IDENTITY` | `nome UNIQUE` | |
| `modulo` | `id INTEGER IDENTITY` | `nome(50) UNIQUE` | |
| `turno` | `id INTEGER IDENTITY` | `nome(50) UNIQUE` | |
| `genero` | `id INTEGER IDENTITY` | `nome(100) UNIQUE` | |

### 2.2 `aluno`

PK: `matricula VARCHAR(5)`.

Campos: `nome_completo`, `foto`, `cpf`, `data_nascimento`, `celular`, `email`, `curso_id` FK, `turno_id` FK, `modulo_id` FK, `cep`, `logradouro`, `complemento`, `bairro`, `localidade`, `uf`, `numero_casa`, `penalidade`, `penalidade_expira_em`, `emprestimos_count DEFAULT 0`, `data_inclusao`.

FKs: `curso`, `turno`, `modulo` (todas `NOT NULL`).

Indices: `idx_aluno_curso`, `idx_aluno_turno`, `idx_aluno_modulo`.

Observacao: `cpf` e `email` **nao** sao `UNIQUE` em V1, mas o servico valida duplicidade no codigo.

### 2.3 `usuario`

PK: `id INTEGER IDENTITY`.

Campos: `email`, `senha`, `role`, `aluno_matricula` FK para `aluno.matricula`.

Observacao: `email` nao e `UNIQUE` em V1, mas o dominio trata como tal.

### 2.4 `livro`

PK: `id BIGINT IDENTITY`.

Campos: `isbn UNIQUE`, `nome`, `data_lancamento`, `numero_paginas`, `cdd_codigo` FK, `editora`, `classificacao_etaria`, `edicao`, `volume`, `quantidade`, `sinopse TEXT`, `autor`, `tipo_capa`, `imagem(5000)`, `avaliacao DOUBLE DEFAULT 4.6`, `data_inclusao`.

FK: `cdd_classificacao`.

Indice: `idx_livro_cdd`.

### 2.5 `livro_genero`

PK composta: (`livro_id`, `genero_id`). Tabela associativa M2M.

### 2.6 `exemplar`

PK: `tombo VARCHAR(10)`.

Campos: `status_livro`, `livro_id` FK `NOT NULL`, `localizacao_fisica NOT NULL`, `data_inclusao`.

Indice: `idx_exemplar_livro`.

### 2.7 `emprestimo`

PK: `id INTEGER IDENTITY`.

Campos: `data_emprestimo NOT NULL`, `data_devolucao NOT NULL`, `penalidade`, `status_emprestimo`, `aluno_matricula` FK, `exemplar_tombo` FK, `renovacoes INTEGER NOT NULL DEFAULT 0` (adicionado em V3).

Indices: `idx_emprestimo_aluno`, `idx_emprestimo_exemplar`, `idx_emprestimo_status`.

### 2.8 `solicitacao_emprestimo`

PK: `id INTEGER IDENTITY`.

Campos: `aluno_matricula` FK, `exemplar_tombo` FK, `data_solicitacao DEFAULT CURRENT_TIMESTAMP`, `status VARCHAR(255) DEFAULT 'PENDENTE'`, `observacao`.

Indices: `idx_solicitacao_aluno`, `idx_solicitacao_status`.

### 2.9 `tcc`

PK: `id BIGINT IDENTITY`.

Campos: `titulo NOT NULL`, `alunos NOT NULL`, `orientadores`, `curso_id INTEGER NOT NULL` FK, `ano_conclusao`, `semestre_conclusao`, `arquivo_pdf`, `foto`, `link_externo`, `ativo DEFAULT TRUE`.

### 2.10 `token_reset_senha`

PK: `id BIGINT IDENTITY`.

Campos: `token UNIQUE NOT NULL`, `usuario_id INTEGER NOT NULL` FK, `data_expiracao NOT NULL`.

### 2.11 `outbox_event` (V2)

PK: `id BIGINT IDENTITY`.

Campos: `event_type(30) NOT NULL`, `recipient_email(255) NOT NULL`, `subject(255) NOT NULL`, `body TEXT NOT NULL`, `status(10) DEFAULT 'PENDING'`, `retry_count DEFAULT 0`, `created_at DEFAULT CURRENT_TIMESTAMP`, `processed_at`, `next_retry_at`.

Indices: `idx_outbox_event_status_created_at`, `idx_outbox_event_status_retry_count`.

### 2.12 `audit_log` (V3)

PK: `id BIGSERIAL`.

Campos: `actor(100) NOT NULL`, `actor_role(50) NOT NULL`, `target_id(200)`, `action(100) NOT NULL`, `result(20) NOT NULL`, `error_message TEXT`, `occurred_at TIMESTAMP NOT NULL`.

Indices: `idx_audit_log_actor`, `idx_audit_log_occurred_at`.

### 2.13 `reserva` (V3)

PK: `id BIGSERIAL`.

Campos: `aluno_id VARCHAR(20) NOT NULL` -> `aluno.matricula`, `livro_id BIGINT NOT NULL` -> `livro.id`, `status(40) DEFAULT 'AGUARDANDO'`, `posicao_fila INTEGER NOT NULL`, `criada_em NOT NULL DEFAULT NOW()`, `expira_em`, `notificado_em`.

Indices: `idx_reserva_livro_status`, `idx_reserva_aluno`.

## 3. Views materializadas (V4)

### 3.1 `mv_dashboard_stats`

Agrega `emprestimos_ativos`, `emprestimos_atrasados`, `emprestimos_concluidos`, `media_dias_devolucao`, `solicitacoes_pendentes`, `reservas_aguardando`.

Fontes: `emprestimo`, `solicitacao_emprestimo`, `reserva`.

Indice unico sobre `(1)` para permitir `REFRESH CONCURRENTLY`.

### 3.2 `mv_top_livros`

Top 20 livros por total de emprestimos. Colunas: `livro_id`, `titulo`, `autor`, `imagem`, `total_emprestimos`, `avaliacao`.

Fontes: `livro` JOIN `exemplar` JOIN `emprestimo`.

### 3.3 `mv_emprestimos_por_mes`

Volume mensal dos ultimos 12 meses. Colunas: `mes DATE`, `total`.

## 4. Caracteristicas transversais observadas

- Tipos temporais usam `TIMESTAMP WITHOUT TIME ZONE`, nao `TIMESTAMPTZ`.
- Nao ha `updated_at` em nenhuma tabela.
- Soft delete nao existe em nenhuma tabela (nem `deleted_at`).
- Strings de status/penalidade persistidas em PT-BR (`'ATIVO'`, `'PENDENTE'`, `'AGUARDANDO'`, etc).
- Contadores derivados persistidos: `aluno.emprestimos_count` e `livro.quantidade`.
- Campos de busca textual nao tem indice trigram/GIN; todas as buscas rodam via `ILIKE` ou `LOWER`.
- Extensoes Postgres nao habilitadas no baseline: `pgcrypto`, `citext`, `pg_trgm`, `unaccent`.

## 5. Enums persistidos (valores encontrados no codigo)

- `Role`: `ADMIN`, `BIBLIOTECARIO`, `ALUNO`.
- `StatusEmprestimo`: `ATIVO`, `CONCLUIDO`, `ATRASADO`.
- `StatusLivro` (exemplar): `DISPONIVEL`, `INDISPONIVEL`, `EM_MANUTENCAO`, `EMPRESTADO`.
- `StatusReserva`: `AGUARDANDO` (default; outros valores existem no dominio).
- `StatusSolicitacao`: `PENDENTE`, `ACEITA`, `REJEITADA`, `CANCELADA`.
- `Penalidade`: `REGISTRO`, `ADVERTENCIA`, `SUSPENSAO`, `BLOQUEIO`, `BANIMENTO`.
- `ClassificacaoEtaria` e `TipoCapa`: definidos em enums Java, valores precisam ser extraidos no PR 5.

## 6. Pontos que a migracao V2 muda

Resumo executivo das mudancas estruturais:

1. Nomes PT-BR -> EN (ver SCHEMA_DICTIONARY.md).
2. Chaves primarias de tabelas de negocio: `UUID` em vez de `SERIAL`/`VARCHAR` (ADR-001).
3. `loan.due_at` separado de `loan.returned_at`.
4. Contadores derivados removidos (ADR-006).
5. `CITEXT` e `UNIQUE` parciais em emails/cpf/isbn.
6. Timestamps viram `TIMESTAMPTZ`.
7. `updated_at` e `deleted_at` adicionados onde faz sentido (soft delete seletivo).
8. Extensoes `pgcrypto`, `citext`, `pg_trgm`, `unaccent` habilitadas.
9. Indices GIN `tsvector` expressao + trigram para busca em `title`/`author`/`full_name`.
10. Constraint parcial unicidade para reserva ativa por (aluno, livro).
11. Valores de enum persistidos em ingles.
12. RLS deny-by-default nas tabelas expostas pelo Data API (ADR-005).
