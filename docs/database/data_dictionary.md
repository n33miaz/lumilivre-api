# Dicionário de Dados — LumiLivre

> Versão: 2026-05-22 — Cobertura completa das tabelas do baseline V1..V5.
> Para diagrama relacional, ver [`ERD.md`](./ERD.md).
> Para DDL exportável, ver [`ddl_standalone.sql`](./ddl_standalone.sql).

Convenções:
- `PK` = primary key, `FK` = foreign key, `UK` = unique key.
- `TIMESTAMPTZ` = `timestamp with time zone` (Postgres).
- `CITEXT` = extensão `citext` (Postgres) — comparação case-insensitive.
- Soft delete (`deleted_at`) ativo apenas em entidades de domínio mutáveis.

---

## 1. Tabelas de referência

### 1.1 `course`

Catálogo de cursos atendidos pela biblioteca. Estática (atualizada por
script administrativo, raramente em runtime).

| Coluna | Tipo            | Constraints                  | Descrição                  |
|--------|-----------------|------------------------------|----------------------------|
| id     | INTEGER         | PK, IDENTITY                 | ID artificial sequencial.  |
| name   | VARCHAR(120)    | NOT NULL, UNIQUE             | Ex.: `Desenvolvimento de Sistemas`. |

### 1.2 `academic_module`

Módulos / etapas do curso (1..4, Básico, Egresso).

| Coluna | Tipo         | Constraints           | Descrição              |
|--------|--------------|-----------------------|------------------------|
| id     | INTEGER      | PK, IDENTITY          | ID artificial.         |
| name   | VARCHAR(50)  | NOT NULL, UNIQUE      | Ex.: `Módulo 2`.       |

### 1.3 `study_shift`

Turnos disponíveis: Matutino, Vespertino, Noturno, Integral.

| Coluna | Tipo         | Constraints           | Descrição              |
|--------|--------------|-----------------------|------------------------|
| id     | INTEGER      | PK, IDENTITY          | ID artificial.         |
| name   | VARCHAR(50)  | NOT NULL, UNIQUE      | Ex.: `Matutino`.       |

### 1.4 `genre`

Gêneros literários atribuíveis a livros via `book_genre`. Cardinalidade
limitada a **3 gêneros por livro** (regra de negócio em `BookService`).

| Coluna | Tipo          | Constraints           | Descrição              |
|--------|---------------|-----------------------|------------------------|
| id     | INTEGER       | PK, IDENTITY          | ID artificial.         |
| name   | VARCHAR(100)  | NOT NULL, UNIQUE      | Ex.: `Romance`.        |

### 1.5 `dewey_classification`

Classificação Decimal de Dewey (CDD). Chave natural pela compatibilidade
com sistemas legados que já operam com a notação `000`-`999`.

| Coluna       | Tipo          | Constraints   | Descrição                          |
|--------------|---------------|---------------|------------------------------------|
| code         | VARCHAR(20)   | PK            | Código CDD (ex.: `005`).           |
| description  | VARCHAR(255)  | —             | Descrição em PT-BR.                |

---

## 2. Pessoas & autenticação

### 2.1 `student`

| Coluna               | Tipo            | Constraints                                   | Descrição                                                     |
|----------------------|-----------------|-----------------------------------------------|---------------------------------------------------------------|
| id                   | UUID            | PK, DEFAULT `gen_random_uuid()`               | Identificador interno.                                        |
| registration_number  | VARCHAR(20)     | NOT NULL, UNIQUE                              | Matrícula. Usada como login e como senha inicial.             |
| full_name            | VARCHAR(255)    | NOT NULL                                      | Nome completo.                                                |
| avatar_url           | VARCHAR(1024)   | —                                             | URL pública/assinada da foto.                                 |
| cpf                  | VARCHAR(11)     | UNIQUE partial (`deleted_at IS NULL`)         | Apenas dígitos; CHECK regex `^[0-9]{11}$`.                    |
| birth_date           | DATE            | —                                             | Data de nascimento.                                           |
| phone_number         | VARCHAR(20)     | —                                             | Apenas dígitos quando preenchido.                             |
| email                | CITEXT          | UNIQUE partial                                | E-mail; comparação case-insensitive.                          |
| course_id            | INTEGER         | NOT NULL, FK→`course(id)` ON DELETE RESTRICT  | Curso atual.                                                  |
| academic_module_id   | INTEGER         | NOT NULL, FK→`academic_module(id)`            | Módulo/etapa atual.                                           |
| study_shift_id       | INTEGER         | NOT NULL, FK→`study_shift(id)`                | Turno atual.                                                  |
| postal_code          | VARCHAR(8)      | —                                             | CEP brasileiro (8 dígitos). Enriquecido via ViaCEP.           |
| street               | VARCHAR(255)    | —                                             |                                                               |
| address_complement   | VARCHAR(55)     | —                                             |                                                               |
| district             | VARCHAR(55)    | —                                             |                                                               |
| city                 | VARCHAR(55)    | —                                             |                                                               |
| state_code           | CHAR(2)         | CHECK regex `^[A-Z]{2}$`                      | UF.                                                           |
| street_number        | INTEGER         | —                                             | Número de porta.                                              |
| penalty_code         | VARCHAR(20)     | —                                             | Valores Java: `RECORD/WARNING/SUSPENSION/BLOCK/BAN`.          |
| penalty_expires_at   | TIMESTAMPTZ     | —                                             | Quando `> now()`, aluno está bloqueado para empréstimos.      |
| created_at           | TIMESTAMPTZ     | NOT NULL, DEFAULT now()                       | Inserção.                                                     |
| updated_at           | TIMESTAMPTZ     | NOT NULL, DEFAULT now(), trigger touch        | Última alteração.                                             |
| deleted_at           | TIMESTAMPTZ     | —                                             | Soft delete.                                                  |

**Invariantes de negócio (camada de serviço):**

- Aluno com `penalty_expires_at > now()` não pode emprestar nem solicitar
  (`RequestApprovalPolicy.validateRequest`).
- Limite default: 3 empréstimos `ACTIVE`+`OVERDUE` por aluno
  (`LoanPolicy.MAX_ACTIVE_LOANS`).

### 2.2 `app_user`

| Coluna           | Tipo          | Constraints                                            | Descrição                                                     |
|------------------|---------------|--------------------------------------------------------|---------------------------------------------------------------|
| id               | UUID          | PK, DEFAULT `gen_random_uuid()`                        | Identificador interno.                                        |
| email            | CITEXT        | NOT NULL, UNIQUE                                       | Login (admin/bibliotecário).                                  |
| password_hash    | VARCHAR(255)  | —                                                      | BCrypt cost 10. Pode ser NULL durante onboarding.             |
| role             | VARCHAR(30)   | NOT NULL, CHECK in (`ADMIN`,`LIBRARIAN`,`STUDENT`)     | Perfil de acesso.                                             |
| student_id       | UUID          | UNIQUE, FK→`student(id)` ON DELETE RESTRICT            | Vinculo 0..1 → 1 com `student` quando `role='STUDENT'`.       |
| preferred_locale | VARCHAR(10)   | NOT NULL, DEFAULT `'pt-BR'`                            | `pt-BR` ou `en-US`. Consumido por `MessageResolver`.          |
| created_at       | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                |                                                               |
| updated_at       | TIMESTAMPTZ   | NOT NULL, DEFAULT now(), trigger touch                 |                                                               |
| deleted_at       | TIMESTAMPTZ   | —                                                      |                                                               |

### 2.3 `password_reset_token`

Token de reset gerado pelo fluxo `/auth/esqueci-senha`. Tempo de vida 30 min.

| Coluna       | Tipo          | Constraints                                | Descrição                          |
|--------------|---------------|--------------------------------------------|------------------------------------|
| id           | BIGINT        | PK, IDENTITY                               | ID artificial.                     |
| token        | VARCHAR(255)  | NOT NULL, UNIQUE                           | UUIDv4 enviado por email.          |
| app_user_id  | UUID          | NOT NULL, UNIQUE, FK→`app_user(id)` ON DELETE CASCADE | Apenas 1 token vivo por usuário.   |
| expires_at   | TIMESTAMPTZ   | NOT NULL                                   | `created_at + 30 min`.             |
| created_at   | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                    |                                    |

---

## 3. Acervo

### 3.1 `book`

Título lógico (não confundir com exemplar físico em `book_copy`).

| Coluna            | Tipo            | Constraints                              | Descrição                                            |
|-------------------|-----------------|------------------------------------------|------------------------------------------------------|
| id                | UUID            | PK, DEFAULT `gen_random_uuid()`          | Identificador.                                       |
| isbn              | VARCHAR(20)     | UNIQUE partial (`deleted_at IS NULL`)    | 10 ou 13 dígitos.                                    |
| title             | VARCHAR(255)    | NOT NULL                                 |                                                      |
| publication_date  | DATE            | CHECK `<= CURRENT_DATE`                  | Data de publicação.                                  |
| page_count        | INTEGER         | CHECK `> 0`                              | Páginas.                                             |
| dewey_code        | VARCHAR(20)     | FK→`dewey_classification(code)` ON DELETE RESTRICT | CDD opcional.                                        |
| publisher         | VARCHAR(120)    | NOT NULL                                 | Editora.                                             |
| age_rating        | VARCHAR(30)     | NOT NULL                                 | Enum Java `AgeRating` (`GENERAL/TEEN/ADULT`).        |
| edition           | VARCHAR(55)     | —                                        | Texto livre.                                         |
| volume            | INTEGER         | —                                        | Volume da coleção quando aplicável.                  |
| synopsis          | TEXT            | —                                        |                                                      |
| author            | VARCHAR(255)    | —                                        | Vários autores separados por `,` ou `;`.             |
| cover_type        | VARCHAR(30)     | —                                        | Enum `CoverType` (`PAPERBACK/HARDCOVER/SOFTCOVER`).  |
| cover_url         | VARCHAR(1024)   | —                                        | URL pública (Supabase Storage ou externa).           |
| rating            | DOUBLE PRECISION| CHECK `>= 0 AND <= 5`, DEFAULT `4.6`     | Avaliação média; default vem da API externa.         |
| created_at        | TIMESTAMPTZ     | NOT NULL, DEFAULT now()                  |                                                      |
| updated_at        | TIMESTAMPTZ     | NOT NULL, DEFAULT now(), trigger touch   |                                                      |
| deleted_at        | TIMESTAMPTZ     | —                                        |                                                      |

### 3.2 `book_genre`

Tabela associativa N:N `book` ↔ `genre`. Regra: até **3 gêneros** por livro.

| Coluna   | Tipo    | Constraints                                                | Descrição |
|----------|---------|------------------------------------------------------------|-----------|
| book_id  | UUID    | PK, FK→`book(id)` ON DELETE CASCADE                        |           |
| genre_id | INTEGER | PK, FK→`genre(id)` ON DELETE RESTRICT                      |           |

### 3.3 `book_copy`

Exemplar físico identificado por `copy_code` (tombo).

| Coluna          | Tipo            | Constraints                                                | Descrição                                                              |
|-----------------|-----------------|------------------------------------------------------------|------------------------------------------------------------------------|
| id              | UUID            | PK, DEFAULT `gen_random_uuid()`                            | Identificador.                                                         |
| copy_code       | VARCHAR(20)     | NOT NULL, UNIQUE                                           | Tombo físico (etiqueta).                                               |
| status          | VARCHAR(20)     | NOT NULL, DEFAULT `'AVAILABLE'`, CHECK in (`AVAILABLE`,`BORROWED`,`UNAVAILABLE`,`MAINTENANCE`) | Estado atual.                                                          |
| book_id         | UUID            | NOT NULL, FK→`book(id)` ON DELETE RESTRICT                 | Pai lógico.                                                            |
| shelf_location  | VARCHAR(255)    | NOT NULL                                                   | Ex.: `A1-01`, `T3-22`.                                                 |
| created_at      | TIMESTAMPTZ     | NOT NULL, DEFAULT now()                                    |                                                                        |
| updated_at      | TIMESTAMPTZ     | NOT NULL, DEFAULT now(), trigger touch                     |                                                                        |
| deleted_at      | TIMESTAMPTZ     | —                                                          |                                                                        |

**Invariantes:**

- Só pode ser emprestado quando `status = AVAILABLE`
  (`BookAvailabilityPolicy.validateAvailable`).
- Ao emprestar → `BORROWED`; ao devolver → `AVAILABLE`.
- Exemplar com empréstimo `ACTIVE`/`OVERDUE` não pode ser excluído.

---

## 4. Operações

### 4.1 `loan`

| Coluna         | Tipo          | Constraints                                                | Descrição                                                            |
|----------------|---------------|------------------------------------------------------------|----------------------------------------------------------------------|
| id             | UUID          | PK, DEFAULT `gen_random_uuid()`                            |                                                                      |
| borrowed_at    | TIMESTAMPTZ   | NOT NULL                                                   | Início do empréstimo.                                                |
| due_at         | TIMESTAMPTZ   | NOT NULL, CHECK `>= borrowed_at`                           | Prazo de devolução.                                                  |
| returned_at    | TIMESTAMPTZ   | CHECK `>= borrowed_at`                                     | Preenchido na devolução.                                             |
| penalty_code   | VARCHAR(20)   | —                                                          | Penalidade aplicada (se houver) na devolução atrasada.               |
| status         | VARCHAR(20)   | NOT NULL, DEFAULT `'ACTIVE'`, CHECK in (`ACTIVE`,`COMPLETED`,`OVERDUE`) |                                                                      |
| student_id     | UUID          | NOT NULL, FK→`student(id)` ON DELETE RESTRICT              | Devedor.                                                             |
| book_copy_id   | UUID          | NOT NULL, FK→`book_copy(id)` ON DELETE RESTRICT            | Exemplar emprestado.                                                 |
| renewal_count  | INTEGER       | NOT NULL, DEFAULT 0, CHECK `>= 0`                          | Quantidade de renovações.                                            |
| created_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                    |                                                                      |
| updated_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT now(), trigger touch                     |                                                                      |

**Invariantes:**

- `LoanPolicy.validateNewLoan`: 3 empréstimos ativos por aluno, sem penalidade.
- `PenaltyPolicy`: faixas de atraso → severidade
  (`<=1d RECORD / 2-5d WARNING / 6-7d SUSPENSION / 8-90d BLOCK / >90d BAN`).
- Renovação: `LoanPolicy.RENEWAL_DAYS = 7` e limite de renovações se houver
  reserva FIFO em fila.

### 4.2 `loan_request`

Solicitação de empréstimo enviada pelo aluno via mobile/web; bibliotecário
aprova ou rejeita.

| Coluna         | Tipo          | Constraints                                                | Descrição                                                            |
|----------------|---------------|------------------------------------------------------------|----------------------------------------------------------------------|
| id             | UUID          | PK, DEFAULT `gen_random_uuid()`                            |                                                                      |
| student_id     | UUID          | NOT NULL, FK→`student(id)` ON DELETE RESTRICT              |                                                                      |
| book_copy_id   | UUID          | NOT NULL, FK→`book_copy(id)` ON DELETE RESTRICT            |                                                                      |
| requested_at   | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                    |                                                                      |
| status         | VARCHAR(20)   | NOT NULL, DEFAULT `'PENDING'`, CHECK in (`PENDING`,`ACCEPTED`,`REJECTED`,`CANCELLED`) |                                                                      |
| note           | VARCHAR(255)  | —                                                          | Texto livre (ex.: `Requested via mobile`).                           |
| created_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                    |                                                                      |
| updated_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT now(), trigger touch                     |                                                                      |

### 4.3 `reservation`

Fila FIFO de reservas para livros sem exemplar disponível.

| Coluna         | Tipo          | Constraints                                                | Descrição                                                             |
|----------------|---------------|------------------------------------------------------------|-----------------------------------------------------------------------|
| id             | UUID          | PK                                                         |                                                                       |
| student_id     | UUID          | NOT NULL, FK→`student(id)`                                 | Posição na fila.                                                      |
| book_id        | UUID          | NOT NULL, FK→`book(id)`                                    | Reserva é do título, não do exemplar.                                 |
| status         | VARCHAR(20)   | NOT NULL, DEFAULT `'WAITING'`, CHECK in (`WAITING`,`READY`,`CANCELLED`,`EXPIRED`,`FULFILLED`) |                                                                       |
| queue_position | INTEGER       | NOT NULL, CHECK `> 0`                                      | Posição na fila do livro.                                             |
| expires_at     | TIMESTAMPTZ   | —                                                          | Preenchido quando passa a `READY` (deadline de retirada).             |
| notified_at    | TIMESTAMPTZ   | —                                                          | Quando o e-mail "livro disponível" foi enviado.                       |
| created_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                    |                                                                       |
| updated_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT now(), trigger touch                     |                                                                       |

### 4.4 `thesis`

TCCs (Trabalhos de Conclusão de Curso) catalogados pela biblioteca.

| Coluna               | Tipo            | Constraints                                            | Descrição                                            |
|----------------------|-----------------|--------------------------------------------------------|------------------------------------------------------|
| id                   | UUID            | PK                                                     |                                                      |
| title                | VARCHAR(255)    | NOT NULL                                               |                                                      |
| authors              | VARCHAR(500)    | NOT NULL                                               | Separados por `;`.                                   |
| advisors             | VARCHAR(500)    | —                                                      |                                                      |
| course_id            | INTEGER         | NOT NULL, FK→`course(id)`                              |                                                      |
| completion_year      | INTEGER         | CHECK `BETWEEN 1900 AND 2100`                          |                                                      |
| completion_semester  | VARCHAR(10)     | —                                                      |                                                      |
| pdf_url              | VARCHAR(1024)   | —                                                      | Upload via Supabase Storage (`theses` bucket).       |
| cover_url            | VARCHAR(1024)   | —                                                      |                                                      |
| external_url         | VARCHAR(1024)   | —                                                      | Quando o PDF estiver em repositório externo.         |
| is_active            | BOOLEAN         | NOT NULL, DEFAULT TRUE                                 | Soft toggle do TCC.                                  |
| created_at, updated_at, deleted_at | TIMESTAMPTZ | padrão                              |                                                      |

---

## 5. Infraestrutura assíncrona

### 5.1 `outbox_event`

Outbox pattern para desacoplar SMTP da transação principal. Publisher
agendado lê pendentes e envia.

| Coluna           | Tipo          | Constraints                                                   | Descrição                                                          |
|------------------|---------------|---------------------------------------------------------------|--------------------------------------------------------------------|
| id               | BIGINT        | PK, IDENTITY                                                  | Append-only.                                                       |
| event_type       | VARCHAR(30)   | NOT NULL                                                      | Enum `OutboxEvent.EventType` (LOAN_CREATED, REQUEST_ACCEPTED…).    |
| recipient_email  | VARCHAR(255)  | NOT NULL                                                      | Destinatário SMTP.                                                 |
| subject          | VARCHAR(255)  | NOT NULL                                                      | Assunto.                                                           |
| body             | TEXT          | NOT NULL                                                      | Corpo.                                                             |
| status           | VARCHAR(20)   | NOT NULL, DEFAULT `'PENDING'`, CHECK in (`PENDING`,`SENT`,`FAILED`,`DEAD_LETTER`) |                                                                    |
| retry_count      | INTEGER       | NOT NULL, DEFAULT 0, CHECK `>= 0`                             | Limite default = 3.                                                |
| created_at       | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                       |                                                                    |
| processed_at     | TIMESTAMPTZ   | —                                                             | Preenchido em `SENT`/`FAILED`/`DEAD_LETTER`.                       |
| next_retry_at    | TIMESTAMPTZ   | —                                                             | Backoff exponencial.                                               |

### 5.2 `audit_log`

Trilha de auditoria das ações administrativas. Preenchida pelo
`AuditAspect` via `@Auditable` em services.

| Coluna         | Tipo          | Constraints                                          | Descrição                                              |
|----------------|---------------|------------------------------------------------------|--------------------------------------------------------|
| id             | BIGINT        | PK, IDENTITY                                         | Append-only.                                           |
| actor          | VARCHAR(100)  | NOT NULL                                             | Email ou matrícula do ator.                            |
| actor_role     | VARCHAR(50)   | NOT NULL                                             | `ADMIN/LIBRARIAN/STUDENT`.                             |
| target_id      | VARCHAR(200)  | —                                                    | ID do recurso afetado (UUID/matrícula/tombo).          |
| action         | VARCHAR(100)  | NOT NULL                                             | Nome do método/operação (`LOAN_CREATED`, etc.).        |
| result         | VARCHAR(20)   | NOT NULL, CHECK in (`SUCCESS`,`FAILURE`,`DENIED`)    |                                                        |
| error_message  | TEXT          | —                                                    | Preenchido quando `FAILURE`/`DENIED`.                  |
| occurred_at    | TIMESTAMPTZ   | NOT NULL                                             | Quando a ação ocorreu (não quando o log foi gravado).  |

---

## 6. Views materializadas (V3)

### 6.1 `mv_dashboard_stats`

1 linha com contadores agregados. Refresh **não-concurrent** (a UNIQUE INDEX
sobre expressão constante `(1)` não habilita `REFRESH CONCURRENTLY`).

| Coluna                | Tipo    | Origem                                                                 |
|-----------------------|---------|------------------------------------------------------------------------|
| active_loans          | BIGINT  | `COUNT(*) FILTER (WHERE loan.status='ACTIVE')`                          |
| overdue_loans         | BIGINT  | `COUNT(*) FILTER (WHERE loan.status='OVERDUE')`                         |
| completed_loans       | BIGINT  | `COUNT(*) FILTER (WHERE loan.status='COMPLETED')`                       |
| avg_return_days       | DOUBLE  | Média de dias `returned_at - borrowed_at` para `status='COMPLETED'`.    |
| pending_requests      | BIGINT  | Sub-SELECT em `loan_request` onde `status='PENDING'`.                   |
| waiting_reservations  | BIGINT  | Sub-SELECT em `reservation` onde `status='WAITING'`.                    |

### 6.2 `mv_top_books`

Top 20 livros mais emprestados. Refresh **concurrent**
(UNIQUE INDEX em `book_id`).

| Coluna       | Tipo                        | Descrição                          |
|--------------|------------------------------|------------------------------------|
| book_id      | UUID                         | FK lógica para `book(id)`.         |
| title        | VARCHAR(255)                 |                                    |
| author       | VARCHAR(255)                 |                                    |
| cover_url    | VARCHAR(1024) (`'' default`) |                                    |
| total_loans  | BIGINT                       | Soma de empréstimos no histórico.  |
| rating       | DOUBLE PRECISION             |                                    |

### 6.3 `mv_loans_by_month`

Volume mensal nos últimos 12 meses. Refresh **concurrent**.

| Coluna  | Tipo         | Descrição                                |
|---------|--------------|------------------------------------------|
| month   | TIMESTAMPTZ  | `DATE_TRUNC('month', loan.borrowed_at)`. |
| total   | BIGINT       | `COUNT(*)`.                              |

---

## 7. Índices relevantes (V4)

> Detalhes em `V4__create_indexes_and_search.sql`. Resumo:

- **Trigram (`pg_trgm` GIN)** em `student.full_name`,
  `book.title`/`book.author`/`book.publisher` para busca ILIKE rápida.
- **B-tree compostos** em `loan(student_id, status, due_at)` e
  `book_copy(book_id, status)`.
- **`UNIQUE INDEX` parcial** em `book.isbn` WHERE `deleted_at IS NULL`
  (permite reusar ISBN de livro deletado).
- **Índice para FK** em todas as colunas `*_id` que referenciam tabelas
  grandes (`loan.student_id`, `loan.book_copy_id`, `loan_request.student_id`,
  etc.).

---

## 8. Triggers

- `touch_updated_at` — função `plpgsql` que atualiza `updated_at := now()`
  antes de cada `UPDATE`. Aplicada via trigger `trg_<tabela>_touch` em
  todas as entidades mutáveis.

---

## 9. Row Level Security

Habilitada via `ENABLE ROW LEVEL SECURITY` + `FORCE` em **todas** as tabelas
do schema `public`. O backend acessa via owner do banco (bypass implícito);
a Data API (PostgREST/Supabase) fica bloqueada por padrão.

Para uma análise completa do modelo de ameaças e por que esse padrão deny-by-default
é o correto para o caso de uso, ver `SECURITY.md` no meta-repo.

---

## 10. Próximos passos

- Adicionar índice GIN em `book_genre(book_id)` para acelerar a query do
  catálogo mobile agrupado por gênero (caso a base ultrapasse 50k livros).
- Considerar `pg_stat_statements` para amostragem de queries lentas em prod.
- Avaliar particionamento de `audit_log` quando ultrapassar ~10M linhas
  (por mês, RANGE em `occurred_at`).
