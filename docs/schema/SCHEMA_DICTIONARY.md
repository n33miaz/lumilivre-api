# Schema Dictionary PT-BR → EN

> Dicionario congelado para a migracao V2. Fonte da verdade para renomear tabelas, colunas, constraints, indices, views materializadas e valores de enum persistidos.
> Toda divergencia vs este documento deve ser corrigida no PR 3 (baseline em ingles) ou registrada em ADR.

## 1. Regra editorial

- Estrutura tecnica (nomes de tabela, coluna, constraint, indice, view, enum value persistido) **em ingles**.
- Conteudo de dominio (nome de curso, genero, titulo, sinopse, TCC, labels de UI) **permanece em PT-BR**.
- Nomes preservados no original quando nao ha equivalente adequado: `cpf`, `cep`, `isbn`, `dewey`.

## 2. Tabelas

| Atual (PT-BR) | Alvo (EN) | Observacao |
|---|---|---|
| `aluno` | `student` | PK muda de `VARCHAR(5)` (matricula) para `UUID`; `registration_number` vira coluna `UNIQUE NOT NULL` |
| `usuario` | `app_user` | Nao usar `user` por ser palavra reservada em Postgres |
| `curso` | `course` | PK `INTEGER` |
| `modulo` | `academic_module` | PK `INTEGER` |
| `turno` | `study_shift` | PK `INTEGER` |
| `genero` | `genre` | PK `INTEGER` |
| `cdd_classificacao` | `dewey_classification` | PK `VARCHAR` (codigo Dewey) |
| `livro` | `book` | PK muda para `UUID`; remove `quantidade` (ADR-006) |
| `livro_genero` | `book_genre` | Tabela associativa M2M |
| `exemplar` | `book_copy` | PK muda para `UUID`; `copy_code` vira coluna `UNIQUE NOT NULL` |
| `emprestimo` | `loan` | PK muda para `UUID`; ganha `returned_at` separado de `due_at` (ADR nova) |
| `solicitacao_emprestimo` | `loan_request` | PK muda para `UUID` |
| `reserva` | `reservation` | PK muda para `UUID` |
| `tcc` | `thesis` | PK muda para `UUID` |
| `token_reset_senha` | `password_reset_token` | PK `BIGINT`; FK `usuario_id` -> `app_user_id` |
| `audit_log` | `audit_log` | PK `BIGINT` |
| `outbox_event` | `outbox_event` | PK `BIGINT` |

## 3. Views materializadas

| Atual | Alvo |
|---|---|
| `mv_dashboard_stats` | `mv_dashboard_stats` |
| `mv_top_livros` | `mv_top_books` |
| `mv_emprestimos_por_mes` | `mv_loans_by_month` |

## 4. Colunas

### 4.1 `student` (`aluno`)

| PT-BR | EN | Observacao |
|---|---|---|
| `matricula` (PK) | `registration_number` `UNIQUE NOT NULL` | PK vira `id UUID` |
| `nome_completo` | `full_name` | |
| `foto` | `avatar_url` | |
| `cpf` | `cpf` | `UNIQUE` parcial `WHERE cpf IS NOT NULL` |
| `data_nascimento` | `birth_date` | |
| `celular` | `phone_number` | |
| `email` | `email` | `CITEXT`, `UNIQUE` parcial |
| `curso_id` (FK) | `course_id` | |
| `turno_id` (FK) | `study_shift_id` | |
| `modulo_id` (FK) | `academic_module_id` | |
| `cep` | `postal_code` | |
| `logradouro` | `street` | |
| `complemento` | `address_complement` | |
| `bairro` | `district` | |
| `localidade` | `city` | |
| `uf` | `state_code` | `CHAR(2)` |
| `numero_casa` | `street_number` | |
| `penalidade` | `penalty_code` | enum traduzido (ver 5.x) |
| `penalidade_expira_em` | `penalty_expires_at` | `TIMESTAMPTZ` |
| `emprestimos_count` | **REMOVIDO** (ADR-006) | Calcular via query/view |
| `data_inclusao` | `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` |
| — | `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` |
| — | `deleted_at` | `TIMESTAMPTZ NULL` (soft delete) |

### 4.2 `app_user` (`usuario`)

| PT-BR | EN | Observacao |
|---|---|---|
| `id` (PK) | `id` `UUID` | Decidido em ADR-001 |
| `email` | `email` | `CITEXT UNIQUE NOT NULL` |
| `senha` | `password_hash` | |
| `role` | `role` | enum traduzido |
| `aluno_matricula` (FK) | `student_id` | `UUID` referenciando `student.id` |
| — | `created_at` | |
| — | `updated_at` | |
| — | `deleted_at` | |

### 4.3 `course` (`curso`)

| PT-BR | EN |
|---|---|
| `id` (PK) | `id` |
| `nome` | `name` |

### 4.4 `academic_module` (`modulo`), `study_shift` (`turno`), `genre` (`genero`)

Mesma forma: `id`, `name`.

### 4.5 `dewey_classification` (`cdd_classificacao`)

| PT-BR | EN |
|---|---|
| `codigo` (PK) | `code` |
| `descricao` | `description` |

### 4.6 `book` (`livro`)

| PT-BR | EN | Observacao |
|---|---|---|
| `id` (PK `BIGINT`) | `id` `UUID` | |
| `isbn` | `isbn` | `UNIQUE` parcial |
| `nome` | `title` | |
| `data_lancamento` | `publication_date` | |
| `numero_paginas` | `page_count` | |
| `cdd_codigo` (FK) | `dewey_code` | |
| `editora` | `publisher` | |
| `classificacao_etaria` | `age_rating` | enum traduzido |
| `edicao` | `edition` | |
| `volume` | `volume` | |
| `quantidade` | **REMOVIDO** (ADR-006) | |
| `sinopse` | `synopsis` | |
| `autor` | `author` | |
| `tipo_capa` | `cover_type` | enum traduzido |
| `imagem` | `cover_url` | |
| `avaliacao` | `rating` | |
| `data_inclusao` | `created_at` | |
| — | `updated_at` | |
| — | `deleted_at` | |

### 4.7 `book_genre` (`livro_genero`)

| PT-BR | EN |
|---|---|
| `livro_id` | `book_id` |
| `genero_id` | `genre_id` |

### 4.8 `book_copy` (`exemplar`)

| PT-BR | EN | Observacao |
|---|---|---|
| `tombo` (PK) | `copy_code` `UNIQUE NOT NULL` | PK vira `id UUID` |
| `status_livro` | `status` | enum traduzido |
| `livro_id` (FK) | `book_id` | `UUID` |
| `localizacao_fisica` | `shelf_location` | |
| `data_inclusao` | `created_at` | |
| — | `updated_at` | |
| — | `deleted_at` | |

### 4.9 `loan` (`emprestimo`)

| PT-BR | EN | Observacao |
|---|---|---|
| `id` (PK) | `id` | `UUID` |
| `data_emprestimo` | `borrowed_at` | `TIMESTAMPTZ` |
| `data_devolucao` | `due_at` | `TIMESTAMPTZ` (prazo) |
| — | `returned_at` | `TIMESTAMPTZ NULL` (devolucao efetiva, ADR separado) |
| `penalidade` | `penalty_code` | |
| `status_emprestimo` | `status` | enum traduzido |
| `aluno_matricula` (FK) | `student_id` | `UUID` referenciando `student.id` |
| `exemplar_tombo` (FK) | `book_copy_id` | `UUID` |
| `renovacoes` | `renewal_count` | |
| — | `created_at` | |
| — | `updated_at` | |

### 4.10 `loan_request` (`solicitacao_emprestimo`)

| PT-BR | EN |
|---|---|
| `id` (PK) | `id` `UUID` |
| `aluno_matricula` | `student_id` |
| `exemplar_tombo` | `book_copy_id` |
| `data_solicitacao` | `requested_at` |
| `status` | `status` |
| `observacao` | `note` |

### 4.11 `reservation` (`reserva`)

| PT-BR | EN |
|---|---|
| `id` (PK) | `id` `UUID` |
| `aluno_id` | `student_id` |
| `livro_id` | `book_id` |
| `status` | `status` |
| `posicao_fila` | `queue_position` |
| `criada_em` | `created_at` |
| `expira_em` | `expires_at` |
| `notificado_em` | `notified_at` |

### 4.12 `thesis` (`tcc`)

| PT-BR | EN | Observacao |
|---|---|---|
| `id` (PK) | `id` `UUID` | |
| `titulo` | `title` | |
| `alunos` | `authors` | Texto livre dos autores (historicamente alunos) |
| `orientadores` | `advisors` | |
| `curso_id` | `course_id` | `NOT NULL` (alinhar com entidade JPA) |
| `ano_conclusao` | `completion_year` | |
| `semestre_conclusao` | `completion_semester` | |
| `arquivo_pdf` | `pdf_url` | |
| `foto` | `cover_url` | |
| `link_externo` | `external_url` | |
| `ativo` | `is_active` | |

### 4.13 `password_reset_token` (`token_reset_senha`)

| PT-BR | EN |
|---|---|
| `id` (PK) | `id` `BIGINT` |
| `token` | `token` `UNIQUE` |
| `usuario_id` | `app_user_id` `UNIQUE` |
| `data_expiracao` | `expires_at` |

### 4.14 `audit_log` (sem traducao de tabela)

| PT-BR | EN |
|---|---|
| `actor` | `actor` |
| `actor_role` | `actor_role` |
| `target_id` | `target_id` |
| `action` | `action` |
| `result` | `result` |
| `error_message` | `error_message` |
| `occurred_at` | `occurred_at` `TIMESTAMPTZ` |

### 4.15 `outbox_event` (sem traducao de tabela)

| PT-BR | EN |
|---|---|
| `event_type` | `event_type` |
| `recipient_email` | `recipient_email` |
| `subject` | `subject` |
| `body` | `body` |
| `status` | `status` |
| `retry_count` | `retry_count` |
| `created_at` | `created_at` |
| `processed_at` | `processed_at` |
| `next_retry_at` | `next_retry_at` |

## 5. Valores de enum persistidos

Plano secao 3.5: codigos tecnicos persistidos ficam em ingles. Labels de UI podem continuar PT-BR.

### 5.1 `Role`

| PT-BR | EN |
|---|---|
| `ADMIN` | `ADMIN` |
| `BIBLIOTECARIO` | `LIBRARIAN` |
| `ALUNO` | `STUDENT` |

### 5.2 `StatusEmprestimo` → `LoanStatus`

| PT-BR | EN |
|---|---|
| `ATIVO` | `ACTIVE` |
| `CONCLUIDO` | `COMPLETED` |
| `ATRASADO` | `OVERDUE` |

### 5.3 `StatusLivro` → `BookCopyStatus`

| PT-BR | EN |
|---|---|
| `DISPONIVEL` | `AVAILABLE` |
| `INDISPONIVEL` | `UNAVAILABLE` |
| `EM_MANUTENCAO` | `MAINTENANCE` |
| `EMPRESTADO` | `BORROWED` |

### 5.4 `StatusReserva` → `ReservationStatus`

| PT-BR | EN |
|---|---|
| `AGUARDANDO` | `WAITING` |
| `DISPONIVEL` | `READY` |
| `CANCELADA` | `CANCELLED` |
| `EXPIRADA` | `EXPIRED` |
| `CONCLUIDA` | `FULFILLED` |

### 5.5 `StatusSolicitacao` → `LoanRequestStatus`

| PT-BR | EN |
|---|---|
| `PENDENTE` | `PENDING` |
| `ACEITA` | `ACCEPTED` |
| `REJEITADA` | `REJECTED` |
| `CANCELADA` | `CANCELLED` |

### 5.6 `Penalidade` → `PenaltyCode`

| PT-BR | EN |
|---|---|
| `REGISTRO` | `RECORD` |
| `ADVERTENCIA` | `WARNING` |
| `SUSPENSAO` | `SUSPENSION` |
| `BLOQUEIO` | `BLOCK` |
| `BANIMENTO` | `BAN` |

### 5.7 `ClassificacaoEtaria` → `AgeRating`

Mapear por faixa quando for confirmado (existe em `LivroModel` mas valor exato nao foi capturado). Proposta:

| PT-BR | EN |
|---|---|
| `LIVRE` | `GENERAL` |
| `DEZ_ANOS` | `AGE_10` |
| `DOZE_ANOS` | `AGE_12` |
| `QUATORZE_ANOS` | `AGE_14` |
| `DEZESSEIS_ANOS` | `AGE_16` |
| `DEZOITO_ANOS` | `AGE_18` |

### 5.8 `TipoCapa` → `CoverType`

| PT-BR | EN |
|---|---|
| `COMUM` | `PAPERBACK` |
| `DURA` | `HARDCOVER` |

### 5.9 `EventType` (outbox) e `OutboxStatus`

Mantidos em ingles no schema atual: `PENDING`, `PROCESSED`, etc. Sem traducao necessaria.

## 6. Nomes de classes Java (PR 5)

Regra: classe Java = versao singular do nome da entidade em ingles + sufixo de camada.

| Atual | Alvo |
|---|---|
| `AlunoModel` | `Student` |
| `UsuarioModel` | `AppUser` |
| `CursoModel` | `Course` |
| `ModuloModel` | `AcademicModule` |
| `TurnoModel` | `StudyShift` |
| `GeneroModel` | `Genre` |
| `CddModel` | `DeweyClassification` |
| `LivroModel` | `Book` |
| `ExemplarModel` | `BookCopy` |
| `EmprestimoModel` | `Loan` |
| `SolicitacaoEmprestimoModel` | `LoanRequest` |
| `ReservaModel` | `Reservation` |
| `TccModel` | `Thesis` |
| `TokenResetSenhaModel` | `PasswordResetToken` |
| `AuditLogModel` | `AuditLog` |
| `OutboxEventModel` | `OutboxEvent` |

Sufixo `Model` removido (convencao Java). Repositories e services seguem mesma logica (`StudentRepository`, `StudentService`).

## 7. Nomes de indices e constraints

Padrao (plano secao 5.3.5):

- `pk_<tabela>` (implicito na maioria dos casos)
- `fk_<tabela>_<coluna_alvo>`
- `uq_<tabela>_<coluna>` ou `uq_<tabela>_<colunas>`
- `ck_<tabela>_<regra>`
- `idx_<tabela>_<colunas>[_<qualificador>]`

Exemplos chave:

- `uq_app_user_email`
- `uq_student_registration_number`
- `uq_book_copy_copy_code`
- `idx_loan_status_due_at`
- `idx_book_title_trgm`
- `idx_reservation_book_id_status_queue_position`

## 8. Contrato HTTP publico (v1)

Decidido em ADR-002: **contrato HTTP permanece em PT-BR na v1**. Payloads JSON e rotas (`/alunos`, `/livros`, `/emprestimos`, etc) nao mudam nesta onda. DTOs da camada de controller fazem o mapping PT-BR (externo) -> EN (interno). Evoluir para v2 em ingles em onda futura.
