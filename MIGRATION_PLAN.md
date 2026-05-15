# MIGRATION_PLAN_V2

> Status: plano atualizado em 2026-05-15 a partir da arvore local. A migracao saiu da fase de planejamento: PR1 a PR3 estao essencialmente materializados, PR4 esta parcial, PR5 esta em andamento avancado, PR6 ainda nao iniciou.
> Objetivo: migrar para um banco novo no Supabase, traduzir o schema para ingles, refatorar o backend para refletir os nomes em ingles de forma desacoplada e documentar o sistema em um nivel que resista bem a leitura tecnica externa.

## 1. Resumo executivo

### 1.1 Stack confirmada no repositorio

- Linguagem: Java 17
- Framework: Spring Boot 3.2.5
- Persistencia: Spring Data JPA + Hibernate
- Migrations: Flyway
- Banco alvo: PostgreSQL no Supabase
- Build: Maven Wrapper
- Seguranca: Spring Security + JWT
- Integracoes relevantes para a migracao: Supabase Storage, JdbcTemplate, materialized views, importacao por Excel

### 1.2 O que esta V2 corrige em relacao ao planejamento anterior

- Adiciona uma fase zero obrigatoria para reconciliar a fonte da verdade do schema. Hoje prompt, entidades JPA e migrations Flyway nao batem 100%.
- Corrige a estrategia de conexao com o Supabase. O backend atual e um servidor persistente, entao nao deve assumir como padrao o pooler transacional na porta `6543`.
- Trata `.env`, `application.properties` local e `application-example.properties` como artefatos diferentes. Hoje o projeto usa variaveis de ambiente; o `.env` nao e carregado automaticamente pelo Spring Boot.
- Separa refatoracao interna do backend de quebra de contrato HTTP. A migracao para ingles deve acontecer primeiro dentro do dominio e da persistencia, com DTOs e endpoints permanecendo estaveis onde isso reduzir risco.
- Introduz testes reais com PostgreSQL. H2 nao e suficiente para validar `ILIKE`, `tsvector`, GIN/GIST, materialized views e comportamento de migrations.
- Troca algumas decisoes amplas demais por regras mais defensaveis, por exemplo: UUID apenas onde faz sentido, `BIGINT` para tabelas append-only, e protecao do Data API do Supabase com RLS deny-by-default.

### 1.3 Premissas operacionais desta V2

- O banco alvo no Supabase sera novo e vazio.
- O projeto pode ganhar um baseline Flyway novo em ingles para o banco novo.
- Se houver dados legados que precisem ser preservados, sera necessario inserir uma fase extra de ETL entre a modelagem e o cutover.
- O contrato HTTP atual pode permanecer em PT-BR na primeira onda para evitar quebrar `lumilivre-web` e `lumilivre-app`.

### 1.4 Checkpoint atual da execucao

Estado validado na arvore local:

- PR1 - discovery/freeze: praticamente concluido. Existem dicionario, diff prompt vs repo e ADRs.
- PR2 - setup de ambiente: concluido ou muito proximo disso. Existem `.env.example`, `application-example.properties` e runbooks.
- PR3 - schema novo em ingles: largamente concluido. As migrations `V1..V5` existem no formato alvo.
- PR4 - testes de banco real: parcial. Existem testes Flyway/schema com Testcontainers, mas o conjunto completo ainda nao esta consolidado em PostgreSQL real.
- PR5 - refatoracao interna do backend: em andamento avancado. Entidades, repositories, storage, dashboard v2 e contratos usados pelos clientes ja foram estabilizados, mas ainda ha residuos v1, DTOs legados, mensagens PT-BR e adapters temporarios.
- PR6 - seed demo e validacao ponta a ponta: nao iniciado. O `V6` atual foi usado para `preferred_locale`, nao para seed demo.
- PR7 - consolidacao documental: parcial. Ha ADRs e runbooks, mas ainda falta fechar documentacao final depois da estabilizacao de contrato.

Conclusao operacional: a proxima frente deve fechar os residuos arquiteturais de PR5 e completar o que falta de PR4 antes de criar seed demo/e2e.

## 2. Fase zero obrigatoria - discovery e freeze do desenho

Esta fase existe porque hoje ha divergencias objetivas entre as tres fontes de schema:

- O DDL enviado no prompt contem colunas como `texto_busca` e `aluno_nome_copia` que nao aparecem no `V1__baseline.sql`.
- O prompt marca alguns campos como unicos e opcionais que a migration baseline nao marca da mesma forma.
- O modelo JPA de `tcc` trata `curso_id` como obrigatorio, enquanto o DDL do prompt o mostra como opcional.
- `usuario.aluno_matricula` e `token_reset_senha.usuario_id` aparecem como `UNIQUE` no prompt, mas isso nao esta refletido no baseline.
- O codigo atual ja depende de views materializadas e SQL nativo com nomes em portugues.

### 2.1 Entregaveis da fase zero

- `docs/schema/source_of_truth_current.md`
- `docs/schema/prompt_vs_repo_diff.md`
- `docs/schema/SCHEMA_DICTIONARY.md`
- Decisao formal de baseline: qual e o schema que sera considerado "estado atual congelado"

### 2.2 Passo a passo

1. Extrair o schema real do ambiente que hoje representa melhor a verdade funcional.
2. Comparar esse dump com:
   - `src/main/resources/db/migration/V1__baseline.sql`
   - `V2__outbox_event.sql`
   - `V3__audit_log_and_reserva.sql`
   - `V4__materialized_views.sql`
   - entidades em `src/main/java/br/com/lumilivre/api/model`
3. Catalogar divergencias de:
   - nomes
   - tipos
   - nullability
   - unicidade
   - FKs
   - indices
   - views
   - colunas derivadas
4. Definir o desenho alvo em ingles antes de tocar no backend.
5. Congelar o dicionario PT -> EN para que nao haja renome tardio no meio da refatoracao.

### 2.3 Gate de saida

- Nao iniciar modelagem final nem refatoracao de backend sem um dicionario aprovado e uma fonte da verdade congelada.

## 3. Principios arquiteturais da migracao

### 3.1 Convencoes de nomenclatura

- Banco:
  - tabelas, colunas, constraints e indices em `snake_case`
  - nomes em ingles
- Codigo Java:
  - classes em PascalCase
  - atributos, metodos e parametros em `camelCase`
  - nomes internos em ingles
- Contrato HTTP v1:
  - pode permanecer em PT-BR no curto prazo
  - deve ser sustentado por DTOs e mapeadores, nao por entidades JPA expostas

### 3.2 Estrategia de IDs

Regra recomendada:

- Tabelas de negocio expostas externamente:
  - `UUID` como PK
- Tabelas de referencia pequenas:
  - `SMALLINT` ou `INTEGER`
- Tabelas append-only de infraestrutura:
  - `BIGINT GENERATED ALWAYS AS IDENTITY`

Aplicacao pratica:

- `student`, `app_user`, `book`, `book_copy`, `loan`, `loan_request`, `reservation`, `thesis` -> `UUID`
- `course`, `academic_module`, `study_shift`, `genre` -> `INTEGER` ou `SMALLINT`
- `audit_log`, `outbox_event` -> `BIGINT`

Justificativa:

- UUID protege melhor IDs externos e desacopla identidade tecnica de chaves naturais.
- Tabelas pequenas de referencia nao ganham nada com UUID.
- Logs e outbox se beneficiam de insercao sequencial e ordenacao temporal natural com `BIGINT`.

### 3.3 Timestamps e auditoria

- Usar `TIMESTAMPTZ` em vez de `TIMESTAMP WITHOUT TIME ZONE`
- Tabelas mutaveis:
  - `created_at NOT NULL DEFAULT now()`
  - `updated_at NOT NULL DEFAULT now()`
- Soft delete apenas onde faz sentido:
  - sim: `student`, `app_user`, `book`, `book_copy`, `thesis`
  - nao: `loan`, `loan_request`, `reservation`, `audit_log`, `outbox_event`, `password_reset_token`

### 3.4 Estrategia de seguranca no Supabase

- Nao usar `service_role` no frontend.
- Habilitar RLS deny-by-default nas tabelas expostas pelo Data API do Supabase, mesmo que a aplicacao principal use JDBC direto.
- Usar buckets publicos apenas para arquivos realmente publicos.
- Usar bucket privado para avatar de aluno e servir via signed URL.

### 3.5 Regra para idioma dos dados

- Estrutura tecnica em ingles:
  - nomes de tabela
  - nomes de coluna
  - nomes internos no codigo
- Conteudo de negocio em portugues:
  - nome de curso
  - genero
  - titulo
  - sinopse
  - TCC
  - dados de seed demo
- Recomendacao adicional:
  - codigos tecnicos persistidos, como status internos, devem ficar em ingles
  - labels exibidas ao usuario podem continuar em PT-BR

Se for necessario manter status persistidos em portugues por compatibilidade, isso deve virar excecao documentada em ADR.

## 4. Fase 1 - Setup inicial do Supabase e configuracao de ambiente

### 4.1 Provisionamento

1. Criar uma organizacao no Supabase para o projeto.
2. Criar dois projetos separados:
   - `lumilivre-dev`
   - `lumilivre-prod`
3. Escolher regiao mais proxima do publico principal.
4. Ativar extensoes necessarias:
   - `pgcrypto`
   - `citext`
   - `pg_trgm`
   - `unaccent`
   - opcional: `btree_gin`

### 4.2 Estrategia correta de conexao

Recomendacao:

- Backend Spring Boot:
  - preferir conexao direta se o ambiente suportar IPv6
  - caso contrario, usar Session pooler `5432`
- Flyway:
  - usar conexao direta ou Session pooler
- Nao usar Transaction pooler `6543` como padrao do backend atual

Motivo:

- O app atual e um servidor persistente com Hibernate e prepared statements.
- O pooler transacional do Supabase e melhor para workloads serverless e conexoes curtas.
- Em `6543`, prepared statements exigem cuidado adicional e podem introduzir ruido desnecessario na primeira migracao.

### 4.3 Variaveis de ambiente

Padrao proposto:

```env
LUMILIVRE_DB_URL_APP=jdbc:postgresql://<host>:5432/postgres?sslmode=require
LUMILIVRE_DB_URL_MIGRATION=jdbc:postgresql://<host>:5432/postgres?sslmode=require
LUMILIVRE_DB_USER=postgres.<project-ref>
LUMILIVRE_DB_PASSWORD=<db-password>

LUMILIVRE_SUPABASE_URL=https://<project-ref>.supabase.co
LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
LUMILIVRE_SUPABASE_ANON_KEY=<anon-key>

LUMILIVRE_JWT_SECRET=<jwt-secret>
LUMILIVRE_JWT_EXPIRATION=86400000

LUMILIVRE_MAIL_HOST=smtp.gmail.com
LUMILIVRE_MAIL_PORT=587
LUMILIVRE_MAIL_USERNAME=<smtp-user>
LUMILIVRE_MAIL_PASSWORD=<smtp-password>

LUMILIVRE_CORS_ORIGINS=http://localhost:5173,http://localhost:8080
```

### 4.4 Regras para `.env`

- O `.env.example` deve usar apenas `KEY=VALUE`.
- Nao usar `export` no template versionado.
- O arquivo `.env` deve ser tratado como helper de shell e `docker --env-file`, nao como fonte magica lida pelo Spring Boot.
- Em PowerShell, as variaveis devem ser carregadas para o ambiente da sessao.
- O `application.properties` local continua lendo `${LUMILIVRE_*}`.

### 4.5 Arquivos que precisam ser alinhados

- `.env.example`
- `src/main/resources/application-example.properties`
- `src/main/resources/application.properties` local e ignorado pelo Git
- `pom.xml` para separar URL da aplicacao e URL de migration
- `README.md` para orientar Git Bash e PowerShell sem ambiguidade

### 4.6 Storage

Buckets propostos:

- `covers` -> publico
- `theses` -> publico se os PDFs puderem ser publicos
- `avatars` -> privado

Estado atual do backend:

- `SupabaseStorageService` usa mapeamento explicito por tipo de asset: `covers`, `theses` e `avatars`.
- Aliases legados `capas` e `tccs` existem apenas para compatibilidade durante o sunset da v1.
- O caminho legado `"alunos"` nao e mais usado como bucket de upload.

### 4.7 Gate de saida da fase 1

- Projeto Supabase criado
- Segredos coletados e documentados
- `application-example.properties` e `.env.example` revisados
- Estrategia de conexao escolhida e registrada em ADR

## 5. Fase 2 - Modelagem e traducao PT -> EN

### 5.1 Dicionario de tabelas recomendado

| Atual | Alvo |
|---|---|
| `aluno` | `student` |
| `curso` | `course` |
| `modulo` | `academic_module` |
| `turno` | `study_shift` |
| `genero` | `genre` |
| `livro` | `book` |
| `livro_genero` | `book_genre` |
| `cdd_classificacao` | `dewey_classification` |
| `exemplar` | `book_copy` |
| `emprestimo` | `loan` |
| `solicitacao_emprestimo` | `loan_request` |
| `reserva` | `reservation` |
| `tcc` | `thesis` |
| `usuario` | `app_user` |
| `token_reset_senha` | `password_reset_token` |
| `audit_log` | `audit_log` |
| `outbox_event` | `outbox_event` |

Views materializadas:

| Atual | Alvo |
|---|---|
| `mv_dashboard_stats` | `mv_dashboard_stats` |
| `mv_top_livros` | `mv_top_books` |
| `mv_emprestimos_por_mes` | `mv_loans_by_month` |

### 5.2 Dicionario de colunas criticas recomendado

#### student

- `matricula` -> `registration_number`
- `nome_completo` -> `full_name`
- `data_nascimento` -> `birth_date`
- `celular` -> `phone_number`
- `email` -> `email`
- `cep` -> `postal_code`
- `logradouro` -> `street`
- `complemento` -> `address_complement`
- `bairro` -> `district`
- `localidade` -> `city`
- `uf` -> `state_code`
- `numero_casa` -> `street_number`
- `penalidade` -> `penalty_code`
- `penalidade_expira_em` -> `penalty_expires_at`
- `foto` -> `avatar_url`

#### book

- `nome` -> `title`
- `data_lancamento` -> `publication_date`
- `numero_paginas` -> `page_count`
- `editora` -> `publisher`
- `classificacao_etaria` -> `age_rating`
- `edicao` -> `edition`
- `sinopse` -> `synopsis`
- `autor` -> `author`
- `tipo_capa` -> `cover_type`
- `imagem` -> `cover_url`
- `avaliacao` -> `rating`
- `cdd_codigo` -> `dewey_code`

#### book_copy

- `tombo` -> `copy_code`
- `status_livro` -> `status`
- `localizacao_fisica` -> `shelf_location`

#### loan

- `data_emprestimo` -> `borrowed_at`
- `data_devolucao` -> `due_at`
- `status_emprestimo` -> `status`
- `renovacoes` -> `renewal_count`

#### loan_request

- `data_solicitacao` -> `requested_at`
- `observacao` -> `note`

#### reservation

- `posicao_fila` -> `queue_position`
- `criada_em` -> `created_at`
- `expira_em` -> `expires_at`
- `notificado_em` -> `notified_at`

#### app_user

- `senha` -> `password_hash`

### 5.3 Melhorias estruturais obrigatorias

1. Separar prazo de devolucao de devolucao efetiva.

Problema atual:

- `emprestimo.data_devolucao` hoje mistura "prazo" com "momento relevante para encerramento".
- Isso contamina relatorios, dashboards e calculo de media de devolucao.

Desenho alvo:

- `borrowed_at`
- `due_at`
- `returned_at`

2. Eliminar contadores derivados sem governanca.

Problemas atuais:

- `aluno.emprestimos_count` e `livro.quantidade` podem derivar de outras tabelas e sofrer drift.
- O codigo atual atualiza isso em services e importacao, espalhando responsabilidade.

Desenho alvo:

- Preferencia 1: remover o dado derivado da tabela base e calcular por query/view.
- Preferencia 2: se for mantido por performance, manter via trigger ou rotina centralizada documentada.

3. Unicidade parcial e case-insensitive onde faz sentido.

- `app_user.email` com `CITEXT` e `UNIQUE`
- `student.email` com `CITEXT` e `UNIQUE` parcial se opcional
- `student.cpf` com `UNIQUE` parcial
- `student.registration_number` com `UNIQUE`
- `book.isbn` com `UNIQUE` parcial
- `book_copy.copy_code` com `UNIQUE`

4. FKs com delete rules explicitas.

Padrao sugerido:

- referencias academicas: `ON DELETE RESTRICT`
- token de reset -> usuario: `ON DELETE CASCADE`
- relacoes historicas de emprestimo, solicitacao e reserva: `ON DELETE RESTRICT`

5. Constraints nomeadas manualmente.

Padrao:

- `pk_*`
- `fk_*`
- `uq_*`
- `ck_*`
- `idx_*`

### 5.4 Estrategia de busca textual

Nao carregar um `tsvector` materializado manualmente sem necessidade.

Desenho recomendado:

- usar expression indexes GIN com `to_tsvector('portuguese', ...)`
- combinar com `unaccent`
- adicionar `pg_trgm` para `ILIKE` performatico em campos como:
  - `student.full_name`
  - `book.title`
  - `book.author`

### 5.5 Indices obrigatorios do desenho alvo

- `idx_student_registration_number`
- `idx_student_full_name_trgm`
- `idx_student_course_module_shift`
- `idx_book_title_trgm`
- `idx_book_author_trgm`
- `idx_book_dewey_code`
- `idx_book_copy_book_id_status`
- `idx_loan_student_id_status_due_at`
- `idx_loan_book_copy_id_status`
- `idx_loan_status_due_at`
- `idx_loan_request_student_id_status_requested_at`
- `idx_reservation_book_id_status_queue_position`
- `idx_outbox_event_status_next_retry_at_created_at`
- `idx_audit_log_occurred_at`

### 5.6 Constraint de negocio importante

Adicionar unicidade parcial para impedir duas reservas ativas do mesmo aluno para o mesmo livro:

- `UNIQUE (student_id, book_id) WHERE status IN (...)`

### 5.7 Estrategia de migrations para o banco novo

Recomendacao:

- Nao continuar do `V1__baseline.sql` atual.
- Criar um baseline novo em ingles para o banco novo.
- Manter o historico legado em branch separada para referencia.

Sequencia planejada originalmente:

1. `V1__create_core_schema_en.sql`
2. `V2__create_outbox_and_audit.sql`
3. `V3__create_materialized_views.sql`
4. `V4__create_indexes_and_search.sql`
5. `V5__seed_reference_data.sql`
6. `V6__seed_demo_data.sql`

Sequencia atual encontrada no repositorio:

1. `V1__create_core_schema_en.sql`
2. `V2__create_outbox_and_audit.sql`
3. `V3__create_materialized_views.sql`
4. `V4__create_indexes_and_search.sql`
5. `V5__seed_reference_data.sql`
6. `V6__add_user_preferred_locale.sql`

Decisao atualizada:

- manter `V6__add_user_preferred_locale.sql`, pois ele pertence ao plano de i18n
- criar o seed demo como uma migration posterior, por exemplo `V7__seed_demo_data.sql`
- se houver migracao de dados reais, criar scripts ETL separados e nao esconder essa complexidade dentro do baseline

## 6. Fase 3 - Refatoracao do backend

### 6.1 Objetivo da fase

Trocar nomes internos em portugues por ingles sem quebrar desnecessariamente:

- frontends existentes
- importacao atual
- seguranca
- dashboard
- integracoes externas

### 6.2 Regra de desacoplamento

Separar claramente quatro camadas:

- HTTP contract
- application/service layer
- domain/persistence model
- infrastructure adapters

Decisao recomendada:

- Controllers e DTOs publicos podem permanecer em PT-BR na v1.
- Services, entities, repositories, queries e nomes de arquivos passam para ingles.
- Mapeadores fazem a traducao entre o contrato externo e o modelo interno.

Isso evita acoplamento entre "nome de coluna", "nome de classe" e "nome de payload JSON".

### 6.3 Ordem segura de execucao

1. Introduzir testes de integracao com PostgreSQL antes do rename estrutural.
2. Congelar o dicionario PT -> EN.
3. Refatorar entidades e repositories.
4. Refatorar SQL nativo e materialized views.
5. Refatorar services e security.
6. Refatorar DTOs internos e mapeadores.
7. Decidir se endpoints e payloads publicos tambem mudarao nesta onda ou em uma `v2` de API.

### 6.4 Hotspots do repositorio que exigem cuidado

Persistencia:

- `src/main/java/br/com/lumilivre/api/model/*`
- `src/main/java/br/com/lumilivre/api/repository/*`

Camada de aplicacao:

- `AlunoService`
- `LivroService`
- `EmprestimoService`
- `ReservaService`
- `SolicitacaoEmprestimoService`
- `UsuarioService`
- `AuthService`
- `ImportacaoService`
- `DashboardService`

Infraestrutura:

- `SupabaseStorageService`
- `EmailService`
- `CepService`
- `GoogleBooksService`
- `BrasilApiService`

Seguranca:

- `StudentAuthorizationService`
- `CanAccessStudent`
- `CanAccessLoan`
- `JwtAuthenticationFilter`
- `CustomUserDetailsService`

HTTP e documentacao:

- `controller/*`
- `dto/*`
- `OpenApiConfig`

Banco e reporting:

- `V4__materialized_views.sql`
- SQL nativo em `AlunoRepository`, `LivroRepository`, `EmprestimoRepository`

### 6.5 Decisao sobre contrato HTTP

Recomendacao de menor risco:

- manter rotas atuais na primeira onda:
  - `/alunos`
  - `/livros`
  - `/emprestimos`
- manter payloads atuais em PT-BR na v1
- migrar primeiro:
  - entidade
  - repository
  - service
  - SQL
  - testes

Depois disso, avaliar:

- expor uma API v2 em ingles
- ou manter PT-BR externamente e ingles apenas internamente

### 6.6 Testes obrigatorios desta fase

- Testes de migration com banco real
- Testes de repository com PostgreSQL
- Testes de contrato dos endpoints criticos
- Testes de dashboard com materialized views reais
- Testes de ownership e autorizacao
- Testes de importacao com cabecalhos em PT-BR

### 6.7 Mudanca de testes recomendada

Estado atual:

- o repositorio ainda depende de H2 em pontos do conjunto de testes
- isso nao valida o desenho do banco alvo

Alvo:

- introduzir Testcontainers PostgreSQL
- validar Flyway + JPA + SQL nativo + views
- deixar H2 apenas para testes realmente unitarios, se ainda fizer sentido

## 7. Fase 4 - Estrategia de seed

### 7.1 Objetivo

Popular o banco novo com dados realistas em portugues sem confundir dado de negocio com estrutura tecnica.

### 7.2 Divisao dos seeds

Seed 1 - referencia:

- cursos
- modulos academicos
- turnos
- generos
- classificacoes Dewey

Seed 2 - demo:

- alunos
- usuarios
- livros
- exemplares
- emprestimos
- solicitacoes
- reservas
- TCCs

### 7.3 Regras de qualidade para seed

- Dados sinteticos, nunca reais
- CPFs validos sinteticos ou mascarados
- emails de dominio controlado, por exemplo `@example.com`
- nomes, cursos, sinopses e titulos em PT-BR
- ids deterministas quando isso ajudar reproducibilidade
- scripts idempotentes com `ON CONFLICT`

### 7.4 Onde colocar o seed

Preferencia:

- seed em SQL versionado pelo Flyway

Alternativas aceitas:

- CSVs lidos por migrations SQL
- script Java apenas se houver necessidade forte de logica gerativa

Evitar:

- seed escondido em `CommandLineRunner`
- seed manual em dashboard

### 7.5 Compatibilidade com a importacao atual

O sistema atual possui `ImportacaoService` e aceita planilhas com cabecalhos em portugues.

Plano recomendado:

- manter a importacao de negocio em PT-BR
- mover a traducao de cabecalho para uma camada de mapping
- permitir que planilhas continuem falando a lingua do usuario final mesmo com schema e codigo internos em ingles

### 7.6 Gate de saida da fase 4

- `course`, `genre`, `book`, `student`, `book_copy`, `loan`, `reservation` e `thesis` conseguem ser carregados do zero
- o dashboard sobe com dados consistentes
- consultas principais retornam dados plausiveis

## 8. Fase 5 - Documentacao

### 8.1 Objetivo

Produzir documentacao que explique as decisoes, nao apenas "como rodar".

### 8.2 Pacote de documentacao recomendado

- `README.md`
  - resumo executivo
  - stack
  - quickstart
  - arquitetura em alto nivel
- `docs/architecture/overview.md`
  - contexto
  - componentes
  - fluxo HTTP -> service -> repository -> DB
- `docs/schema/SCHEMA_DICTIONARY.md`
  - tabela PT -> EN
  - coluna PT -> EN
  - observacoes semanticas
- `docs/schema/ERD.md`
  - diagrama Mermaid do modelo alvo
- `docs/runbooks/supabase_setup.md`
  - provisionamento
  - secrets
  - connection strategy
- `docs/runbooks/local_environment.md`
  - PowerShell
  - Git Bash
  - `.env`
  - `application.properties`
- `docs/adr/`
  - ADR 001 - ID strategy
  - ADR 002 - API contract strategy
  - ADR 003 - Search strategy
  - ADR 004 - Supabase connection strategy
  - ADR 005 - RLS strategy
  - ADR 006 - Counter and derived field strategy

### 8.3 O que precisa estar explicitamente documentado

- por que o schema foi traduzido
- por que alguns dados continuam em PT-BR
- por que o contrato HTTP pode continuar em PT-BR na v1
- como o Supabase foi configurado
- como rodar migrations
- como rodar testes de banco real
- como regenerar o ambiente do zero
- quais tradeoffs foram aceitos

### 8.4 Regra editorial

- Nao escrever documentacao "para recrutadores".
- Escrever para:
  - maintainers
  - contributors
  - reviewers tecnicos
  - futuros engenheiros do projeto

## 9. Sequencia recomendada de execucao em PRs

### PR 1 - discovery e freeze

Status: essencialmente concluido.

- dicionario PT -> EN
- diff prompt vs repo
- ADRs iniciais

### PR 2 - setup de ambiente

Status: concluido ou quase concluido.

- `.env.example`
- `application-example.properties`
- `README.md`
- runbooks de Supabase e ambiente local

### PR 3 - schema novo em ingles

Status: largamente concluido.

- baseline Flyway novo
- indices
- views
- seeds de referencia

### PR 4 - testes de banco real

Status: parcial.

- Testcontainers PostgreSQL
- smoke tests de Flyway
- repository tests

Pendencia atual:

- ampliar alem de migration/schema smoke para repository tests e fluxos que dependem de PostgreSQL real

### PR 5 - refatoracao interna do backend

Status: em andamento avancado.

- entidades
- repositories
- services
- SQL nativo
- dashboard
- storage
- contratos v2 usados por web/app

Pendencia atual:

- remover residuos de DTOs v1 nos fluxos v2
- eliminar mensagens PT-BR hardcoded em services
- concluir a substituicao de adapters temporarios v1 em fluxos v2
- fechar OpenAPI v2 como contrato oficial

### PR 6 - seed demo e validacao ponta a ponta

Status: nao iniciado.

- dados demo
- importacao
- testes de dashboard

Nota: o numero `V6` ja foi ocupado por `V6__add_user_preferred_locale.sql`; o seed demo deve entrar em uma migration posterior.

### PR 7 - consolidacao documental

Status: parcial.

- ERD
- ADRs finais
- README final

## 10. Checklist de aceite

- [x] Banco novo sobe do zero apenas com Flyway em ingles para schema base.
- [ ] `mvn test` cobre a nova camada de persistencia com PostgreSQL real de ponta a ponta.
- [ ] API v1 continua funcional para web e mobile enquanto a v2 e estabilizada.
- [ ] Todos os nomes estruturais relevantes do banco estao em ingles.
- [ ] Dados de seed demo de negocio permanecem em portugues.
- [ ] Views materializadas e queries nativas foram renomeadas e validadas.
- [x] Storage diferencia capas, TCCs e avatars corretamente.
- [ ] RLS e estrategia de acesso ao Supabase foram documentadas e revisadas.
- [ ] A documentacao explica as decisoes e nao apenas os comandos.

## 11. Decisoes finais recomendadas

Se fosse meu plano de execucao, eu fixaria estas decisoes agora:

1. Banco novo com baseline Flyway novo em ingles.
2. `public API v1` mantida em PT-BR no curto prazo.
3. Nomes internos do backend integralmente em ingles.
4. PostgreSQL real via Testcontainers antes da refatoracao pesada.
5. Conexao direta ou Session pooler `5432` como padrao, nao `6543`.
6. `RLS deny-by-default` para defesa em profundidade.
7. `loan.due_at` e `loan.returned_at` separados.
8. Contadores derivados removidos ou governados por trigger/view.

## 12. Referencias operacionais

- Supabase connection strings and poolers:
  - https://supabase.com/docs/reference/postgres/connection-strings
- Supavisor terminology:
  - https://supabase.com/docs/guides/troubleshooting/supavisor-and-connection-terminology-explained-9pr_ZO
- Supabase storage buckets:
  - https://supabase.com/docs/guides/storage/buckets/fundamentals
- Supabase secure data and RLS:
  - https://supabase.com/docs/guides/database/secure-data
  - https://supabase.com/docs/guides/database/postgres/row-level-security
