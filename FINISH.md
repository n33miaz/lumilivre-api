# LumiLivre — Plano de Finalização (FINISH)

> Plano executável para fechar o ecossistema LumiLivre antes do lançamento open-source `0.1.0`.
> Foco: **modularização**, **DX**, **observabilidade**, **cobertura de testes**, e **diferenciais que recrutadores Tier 1 esperam ver** (Itaú, Nubank, Cielo, Stone, MercadoLibre, Stripe, etc.).
>
> Princípio inegociável: **nada que já funciona pode quebrar**. Cada fase tem critério de aceitação verificável (testes + smoke manual).
>
> Status base: 2026-05-22. Branch alvo: `master` no meta + `main` em cada sub-repo.
> Convenção de commits: Conventional Commits, título ≤ 30 chars, mensagens em inglês.

---

## Sumário executivo

| Fase | Tema | Bloqueia release? | Esforço | Pri |
|------|------|---|---|---|
| F0 | Hotfixes pendentes (seed, secrets) | Sim | 1-2h | 🔴 P0 |
| F1 | DX local + setup sem Supabase | Sim | 1d | 🔴 P0 |
| F2 | Modularização: Storage Provider | Não, mas é o headline | 1-2d | 🟡 P1 |
| F3 | Modularização: Book Metadata Providers | Não, mas é o headline | 2d | 🟡 P1 |
| F4 | Modularização: Postal Code Provider | Não, mas é o headline | 1d | 🟡 P1 |
| F5 | Eliminar PT-BR hardcoded em services | Sim para release bilíngue | 1-2d | 🟡 P1 |
| F6 | Expandir seed demo (todos status, capas, paginação) | Sim para "abra e use" | 4-6h | 🟡 P1 |
| F7 | Documentação de banco (ER, DDL standalone, dicionário, migração) | Sim para adoção | 1d | 🟡 P1 |
| F8 | Cobertura de testes (WireMock, Testcontainers E2E, services não testados) | Não, mas dobra credibilidade | 3-5d | 🟢 P2 |
| F9 | Polish OSS (OpenAPI 100%, CHANGELOG, LICENSE meta, templates, Dependabot) | Sim para release | 1d | 🟢 P2 |
| F10 | Diferenciais "wow" (k6 perf, OpenTelemetry, GraalVM, k8s, archi tests) | Não, **mas é o que vende** | 2-4d | 🟢 P3 |

**Caminho crítico para `0.1.0` público:** F0 → F1 → F5 → F6 → F7 → F9. **Caminho que impressiona recrutador:** F2 → F3 → F4 → F8 → F10.

---

## F0 — Hotfixes pendentes

### Objetivo
Corrigir os bugs residuais que sobraram após a última rodada de ajustes do schema/seed.

### Por que importa
Bug silencioso em seed demo significa que o primeiro `make seed` de qualquer contribuidor vai ter dados quebrados (FK NULL, livros sem gênero). Recrutador clona, roda, vê erro: descarta.

### Estado já corrigido (pelo usuário)
- `'Administracao'`/`'Administração'` em `lumilivre-api/src/main/resources/db/seed/R__seed_demo_data.sql`
- `'Modulo X'`/`'Módulo X'` em V5 + R__seed
- `preferred_locale` reorganizado dentro do bloco de colunas em `V1__create_core_schema_en.sql:121`
- `FlywayMigrationTest` agora espera `>= 5` migrations (`src/test/java/br/com/lumilivre/api/migration/FlywayMigrationTest.java:54`)
- `DashboardService.java:79` — remoção de `CONCURRENTLY` do `mv_dashboard_stats` (única view sem `UNIQUE INDEX` no `(1)` permite refresh non-concurrent só)
- Reorganização de docs: ADRs do api migrados para o meta, criado `lumilivre-api/docs/database/supabase_query.md`

### Tarefas restantes
- [ ] **Bug "Didatico"**: `R__seed_demo_data.sql:276` referencia `'Didatico'`; V5 insere `'Didático'` (`V5__seed_reference_data.sql:73`). Atualizar o R__seed para `'Didático'`.
- [ ] **`lumilivre-key.pem` no tracking**: arquivo presente em `C:\dev\outros\lumilivre\lumilivre-key.pem`. Confirmar via `git ls-files` em cada repo, rotacionar a chave correspondente, remover do tracking via `git rm --cached`. Adicionar regra em `.gitignore` (já existe a regra `*.pem` segundo o `opensource.md` §7, validar).
- [ ] **`Dockerfile -DskipTests`**: `lumilivre-api/Dockerfile:5` pula testes no build de imagem. Manter o skip (build rápido em deploy) é OK, mas garantir que CI roda `./mvnw verify` antes do `docker build` no workflow.
- [ ] **Typos do EmailService**: `EmailService.java:21` tem `contato.lumlivre@gmail.com.br` (falta o "i" de "livre") e `:22` tem `https://www.lumilivre.com.br` hardcoded. Mover para `application.properties` (`app.email.from`, `app.public-url`) com defaults sensatos.

### Critério de aceitação
- `./mvnw test` verde sem skips.
- `git grep -n "Didatico" lumilivre-api/src/main/resources/db/` retorna 0 ocorrências.
- `git ls-files | grep "\.pem$"` retorna 0 ocorrências em todos os 4 repos.
- Restart `OutboxPublisherService` envia email com o domínio correto.

---

## F1 — Developer Experience local + Setup sem Supabase

### Objetivo
Permitir que qualquer pessoa clone o repo e tenha API + DB + dados demo + cache rodando em **um único comando**, sem precisar criar conta Supabase.

### Por que importa
Hoje, `docker-compose.yml` em `lumilivre-api/` sobe **só Redis**. Para rodar a API o contribuidor precisa abrir conta Supabase, criar projeto, configurar 8 variáveis de ambiente. Isso é hostil. Em contraste, projetos Tier 1 OSS (Hasura, Supabase próprio, Plausible, Cal.com) sobem com `docker compose up` em < 60s.

Recrutador olha o README, tenta rodar, leva 30 minutos vs 1 minuto: a primeira impressão decide.

### Tarefas
- [ ] **`lumilivre-api/docker-compose.yml`** estender para incluir:
  - `postgres:16-alpine` com `pg_trgm`, `pgcrypto`, `citext`, `unaccent` habilitados via init script `docker/postgres/init.sql`
  - `redis:alpine` (já existe)
  - `mailhog/mailhog` para capturar emails em dev (acesso em http://localhost:8025)
  - `minio/minio` como S3-compatible substituto do Supabase Storage em dev (depende de F2 estar pronto; pode ficar comentado até lá)
  - Volume persistente para o Postgres
  - Healthcheck em todos os serviços
- [ ] **`docker-compose.override.yml`** com a app Spring Boot containerizada para subida full-stack via `docker compose up`.
- [ ] **Profile `local`** em `application-local.properties`: aponta para `localhost:5432`, Mailhog `localhost:1025`, Redis `localhost:6379`, MinIO `localhost:9000`. Não exige `LUMILIVRE_*` secrets.
- [ ] **`Makefile` no root do meta** com targets:
  - `make setup` — `cp .env.example .env` nos 4 repos + `docker compose up -d` + `mvn flyway:migrate` + seed
  - `make api` — sobe só a API local
  - `make web` — sobe só o web local
  - `make app` — `flutter run --flavor dev`
  - `make seed` — re-aplica `R__seed_demo_data.sql` (repeatable)
  - `make clean` — `docker compose down -v` + `mvn clean`
  - `make test` — roda toda a suíte (api + web + app)
- [ ] **`Makefile.windows.ps1`** ou substituir Make por `just` (cross-platform). Recomendado: `just` — mais moderno, suporte nativo Windows.
- [ ] **`.devcontainer/devcontainer.json`** no meta para GitHub Codespaces 1-click. Inclui Java 17, Node 20, Flutter 3, Docker-in-Docker.
- [ ] **Quickstart no `README.md` do meta**: 5 linhas, do clone ao login funcionando.
  ```bash
  git clone --recurse-submodules https://github.com/n33miaz/lumilivre.git
  cd lumilivre
  just setup
  just api  # outra aba: just web
  open http://localhost:5173  # admin@lumilivre.test / admin@lumilivre.test
  ```

### Critério de aceitação
- Em máquina limpa (VM Ubuntu), `just setup && just api` resulta em API saudável em `http://localhost:8080/actuator/health` em < 90s.
- Login funciona com as 3 contas demo seedadas.
- Nenhuma variável `LUMILIVRE_*` precisa ser definida manualmente.
- Documentação atualizada em `README.md` (meta) + `lumilivre-api/README.md` com a seção "Sem Supabase, em 60 segundos".

### Riscos
- Postgres local não tem RLS deny-by-default pré-configurado igual ao Supabase. **Mitigação:** init script ativa as mesmas policies.
- Devs com M1/M2: imagens precisam ser multi-arch. Usar `--platform linux/amd64,linux/arm64` no docker-compose.

---

## F2 — Modularização: Storage Provider

### Objetivo
Extrair uma interface `StorageProvider` para que qualquer um possa trocar Supabase Storage por **S3, GCS, MinIO, Cloudinary, R2** sem tocar em `BookService`, `StudentService`, `ThesisService`.

### Por que importa para recrutador
Demonstra:
- **Strategy Pattern** + **Dependency Inversion (SOLID)** aplicados em código real
- Capacidade de **identificar acoplamento implícito** (3 services importando classe concreta)
- Engenharia para extensibilidade — chave em OSS

### Estado atual
- `SupabaseStorageService.java:22` é classe concreta sem interface
- Importadores diretos:
  - `BookService.java:47`
  - `StudentService.java:37`
  - `ThesisService.java:17`
- Buckets já têm nomes neutros (`covers`, `theses`, `avatars`) — bom sinal

### Tarefas
- [ ] Criar `br.com.lumilivre.api.service.infra.storage.StorageProvider`:
  ```java
  public interface StorageProvider {
      String upload(MultipartFile file, StorageBucket bucket);
      void delete(String objectPath, StorageBucket bucket);
      InputStream download(String objectPath, StorageBucket bucket);
      String signedUrl(String objectPath, StorageBucket bucket, Duration ttl);
  }
  ```
- [ ] Enum `StorageBucket` { `COVERS`, `THESES`, `AVATARS` } substitui a string `"covers"`/`"theses"`/`"avatars"` espalhada hoje.
- [ ] Renomear `SupabaseStorageService` → `SupabaseStorageProvider implements StorageProvider`. Marcar com `@ConditionalOnProperty(name = "lumilivre.storage.provider", havingValue = "supabase", matchIfMissing = true)`.
- [ ] Criar `S3StorageProvider` (AWS SDK v2) — `@ConditionalOnProperty(name = "lumilivre.storage.provider", havingValue = "s3")`.
- [ ] Criar `MinioStorageProvider` — usa AWS SDK v2 contra endpoint MinIO. Mesmo bean condicional `havingValue = "minio"`.
- [ ] Criar `LocalFilesystemStorageProvider` — escreve em `./storage/` local. Útil em dev sem Docker. `havingValue = "local"`.
- [ ] Refatorar `BookService`, `StudentService`, `ThesisService` para depender de `StorageProvider` (interface).
- [ ] Atualizar `application.properties` com `lumilivre.storage.provider=${LUMILIVRE_STORAGE_PROVIDER:supabase}` e props por provider (`lumilivre.storage.s3.region`, `lumilivre.storage.s3.bucket-covers`, etc.).
- [ ] **Migrar `@Value` espalhados**: hoje `BookService.java:62` lê `${supabase.storage.base-url-capas}` diretamente. Mover para dentro do `StorageProvider` (retorne URL pública na resposta do `upload`).
- [ ] Adicionar testes:
  - `StorageProviderContractTest` — abstrato, cada impl extende e roda
  - `SupabaseStorageProviderIntegrationTest` — usa WireMock para simular Supabase
  - `MinioStorageProviderIntegrationTest` — Testcontainers com MinIO
  - `LocalFilesystemStorageProviderTest` — escrita real em `@TempDir`
- [ ] **ADR-011**: registrar a decisão de strategy + matriz de providers suportados.

### Critério de aceitação
- Trocar `LUMILIVRE_STORAGE_PROVIDER=supabase` para `=minio` em ambiente local não exige recompilação nem alteração de código.
- `BookService`/`StudentService`/`ThesisService` não importam mais nada de `supabase.*`.
- `git grep -l "SupabaseStorageService" lumilivre-api/src/main/java/` retorna apenas a impl + classe condicional de config.
- Testes de contrato passam para todos os 4 providers.

### Riscos
- AWS SDK v2 adiciona ~12 MB ao fat jar. **Mitigação:** marcar S3 dependency como `<optional>true</optional>` no `pom.xml`, igual ao Redis hoje.

---

## F3 — Modularização: Book Metadata Providers

### Objetivo
Tornar **plugável** a busca de metadados de livros por ISBN. Hoje só Google Books + BrasilAPI estão hardcoded em `BookService.preencherDadosExternos()`. Permitir OpenLibrary, ISBNdb, WorldCat, Library of Congress, Deutsche Nationalbibliothek, Casas del Libro (ES) etc.

### Por que importa
- **Diferencial fundamental para internacionalização**: bibliotecas fora do Brasil não vão usar BrasilAPI. Sem isso, o projeto é "uma solução brasileira" em vez de uma **plataforma global**.
- Para recrutador: combina **Strategy** + **Chain of Responsibility** + **Composite Pattern** em uma feature de negócio real.

### Estado atual
- `GoogleBooksService.java` e `BrasilApiService.java` são classes concretas sem interface comum
- DTOs específicos (`GoogleBooksResponse`, `BrasilApiResponse`) vazam para o resto do sistema
- `BookService.java:263-323` (`preencherDadosExternos`) tem `if/else` hardcoded: Google primeiro, BrasilAPI como fallback
- Cada provider já tem seu próprio circuit breaker em `application.properties:64-86` (bom)

### Tarefas
- [ ] DTO interno comum:
  ```java
  public record BookMetadata(
      String isbn, String title, String publisher, String synopsis,
      List<String> authors, LocalDate publicationDate, Integer pageCount,
      String coverUrl, Double rating, List<String> categories,
      String providerName  // rastreabilidade
  ) {}
  ```
- [ ] Interface:
  ```java
  public interface BookMetadataProvider {
      String name();   // "googleBooks", "brasilApi", "openLibrary", ...
      Set<Locale> supportedLocales();  // [pt-BR, en-US, ...] — guidance pra picker
      Optional<BookMetadata> findByIsbn(String isbn);
      Optional<BookMetadata> findByTitleAndAuthor(String title, String author);
  }
  ```
- [ ] `BookMetadataChain` (Spring bean):
  - Injeta `List<BookMetadataProvider>` (Spring popula com todos os beans descobertos)
  - Ordena segundo `lumilivre.book-metadata.providers=googleBooks,openLibrary,brasilApi` (lista ordenada via config)
  - `findByIsbn` itera, retorna o primeiro `present`. Faz **merge inteligente** de campos faltantes do segundo provider (Google sem capa? Procura no OpenLibrary)
- [ ] Refatorar `BookService.preencherDadosExternos` para depender só de `BookMetadataChain`. Lógica de merge desaparece do service.
- [ ] Renomear e adaptar:
  - `GoogleBooksService` → `GoogleBooksProvider implements BookMetadataProvider` — converte `VolumeInfo` em `BookMetadata`
  - `BrasilApiService` → `BrasilApiProvider implements BookMetadataProvider`
- [ ] Novo: `OpenLibraryProvider` — API gratuita, multi-idioma, cobre bibliografia global. Endpoint `https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data`.
- [ ] (Opcional) `IsbnDbProvider` — pago mas excelente; fica desabilitado por padrão.
- [ ] Cada provider mantém seu próprio `@CircuitBreaker(name = "{providerName}")`.
- [ ] Adicionar config:
  ```properties
  lumilivre.book-metadata.providers=${LUMILIVRE_BOOK_METADATA_PROVIDERS:googleBooks,openLibrary,brasilApi}
  lumilivre.book-metadata.merge-strategy=${LUMILIVRE_BOOK_METADATA_MERGE:fill-missing}
  ```
- [ ] Testes:
  - Contract test abstrato `BookMetadataProviderContractTest`
  - `BookMetadataChainTest` — verifica ordem, fallback, merge
  - WireMock test por provider com fixtures reais
- [ ] **ADR-012**: registrar Strategy + Chain decision.

### Critério de aceitação
- Adicionar `OpenLibraryProvider` não exige alterar `BookService`.
- `LUMILIVRE_BOOK_METADATA_PROVIDERS=openLibrary` (só ele) faz cadastro de livro funcionar usando apenas OpenLibrary.
- WireMock simula Google fora do ar; sistema cai automaticamente para BrasilAPI/OpenLibrary.
- Cobertura nas policies de merge ≥ 90%.

---

## F4 — Modularização: Postal Code Provider

### Objetivo
Tornar o sistema **internacional para endereços**. Hoje ViaCEP só funciona para Brasil (8 dígitos).

### Por que importa
- Bibliotecas escolares no exterior não têm CEP brasileiro.
- Para recrutador: mostra que você **anteciparia internacionalização** sem precisar de instrução.
- Bandeira de **inclusão** no projeto OSS.

### Estado atual
- `CepService.java:12` chama `https://viacep.com.br/ws/{cep}/json/` direto
- `MetadataController.java:94` rejeita `length != 8` (rejeita ZIP US, UK postcode, etc.)
- DTO `AddressLookupResponse` com campos PT (`logradouro`, `bairro`, `localidade`, `uf`)
- **Sem Resilience4j** (diferente de Google/BrasilAPI) — sem retry/circuit breaker/timeout
- Sem cache (cada lookup vai ao ViaCEP)

### Tarefas
- [ ] DTO genérico:
  ```java
  public record PostalAddress(
      String postalCode, String countryCode, // ISO 3166-1 alpha-2
      String streetLine, String district, String city, String region,
      String regionCode, // ex.: "SP", "CA", "BY"
      Double latitude, Double longitude
  ) {}
  ```
- [ ] Interface:
  ```java
  public interface PostalCodeProvider {
      String name();
      Set<String> supportedCountryCodes();  // ["BR"], ["US"], ["*"] = universal
      Optional<PostalAddress> lookup(String code, String countryCode);
  }
  ```
- [ ] `BrazilianPostalCodeProvider` (ViaCEP) — atual `CepService` renomeado e adaptado. Aceita só `BR`.
- [ ] `UniversalPostalCodeProvider` (Zippopotam.us — `https://api.zippopotam.us/{country}/{postcode}`, cobre ~60 países, gratuito, sem auth).
- [ ] `PostalCodeRouter`: Spring bean que recebe `(code, countryCode)`, escolhe provider apropriado pela `supportedCountryCodes()`.
- [ ] Adicionar Resilience4j ao novo `CepService` (alinha com pattern dos demais):
  ```properties
  resilience4j.circuitbreaker.instances.postalCode.slidingWindowSize=10
  resilience4j.retry.instances.postalCode.maxAttempts=2
  resilience4j.timelimiter.instances.postalCode.timeoutDuration=2s
  ```
- [ ] Adicionar cache TTL 7 dias (endereços não mudam) — `@Cacheable("postal-codes")` em chave composta `(countryCode, code)`.
- [ ] Atualizar `MetadataController.postalCode`:
  - Aceitar query param `?country=BR` (default), `?country=US`, etc.
  - Validação de formato delegada ao provider (regex por país).
- [ ] Adicionar coluna `student.country_code CHAR(2) NOT NULL DEFAULT 'BR'` via `V6__student_country_code.sql`.
- [ ] Web/App: passar `country` automaticamente baseado no aluno selecionado.
- [ ] Testes WireMock por provider.

### Critério de aceitação
- `GET /api/metadata/postal-codes/90210?country=US` retorna `{ city: "Beverly Hills", regionCode: "CA" }`.
- ViaCEP fora do ar: log estruturado + fallback graceful (formulário continua submitable, só não autopreenche).
- Cache hit rate observável em `/actuator/prometheus`.

---

## F5 — Eliminar PT-BR hardcoded em services

### Objetivo
Fechar a Etapa A do `IDIOMES.md` §5: todo output ao usuário precisa sair via `MessageResolver`/`messages` bundles.

### Por que importa
A própria meta-doc `IDIOMES.md` declara que `string hardcoded em fluxo novo e regressao` (§1.5). Hoje há ~30 ocorrências em services. Antes do lançamento bilíngue, isso vira inconsistência visível (admin troca para EN, recebe email em PT).

### Tarefas
- [ ] **`LoanService`** — `LoanService.java:182-184, 238-240, 374-390`:
  - Mover bodies/subjects de email para `i18n/loan/messages_{locale}.properties`
  - Usar `messages.resolve("loan.email.created.subject", locale, ...)` + `args`
- [ ] **`LoanRequestService`** — `LoanRequestService.java:76, 101, 134, 141`:
  - Idem para `request.email.accepted.subject`, `.rejected.subject`, etc.
- [ ] **`EmailService`** — `EmailService.java:21-22, 62-89`:
  - Mover `FROM`, `SITE` para `application.properties` (`app.email.from`, `app.public-url`)
  - Templates HTML viram Thymeleaf em `resources/templates/email/{name}_{locale}.html`
  - Subject vem do bundle
- [ ] **`ReportService`** — `ReportService.java:68-320`:
  - Cabeçalhos, colunas de PDF, rodapés todos via `messages.resolve("report.loans.header", locale)`
  - Suporte `Accept-Language` no endpoint `/api/reports/**`
- [ ] **`ImportService`** — `ImportService.java:80-369`:
  - Mensagens de erro de importação XLSX para `i18n/import/messages_*.properties`
  - DTO `ErroImportacao` ganha `messageKey` em vez de string
- [ ] **`BusinessMetricsService`** — `BusinessMetricsService.java:28-46`:
  - `description` do Micrometer continua em inglês (convenção Prometheus); apenas remover acentos (`Numero` em vez de `Número`)
- [ ] **`AppUserService`** — `:79`: `"Administrador"`/`"Bibliotecário"` viram `enum Role.getLocalizedLabel(locale)`
- [ ] **`BookService`** — `:213, 247, 376`: mensagens de erro em `RuntimeException` viram `BusinessRuleException.ofKey("book.create.failed", ...)` (mesmo padrão dos outros)
- [ ] Atualizar script `lumilivre-api/scripts/check-i18n-coverage.sh` para checar `services/` também — hoje só checa `controllers/`.

### Critério de aceitação
- `grep -rE '"[^"]*[À-ÿ]+[^"]*"' lumilivre-api/src/main/java/br/com/lumilivre/api/service/` retorna 0 ocorrências relevantes (logs internos podem permanecer).
- Email de empréstimo em conta com `preferred_locale='en-US'` chega em inglês.
- PDF gerado com `Accept-Language: en-US` tem cabeçalhos em inglês.

---

## F6 — Expandir seed demo

### Objetivo
Cobrir **todos os status de todas as entidades** + capas reais + paginação. Permitir que qualquer um, ao subir o sistema, exercite **100% da UI** sem cadastrar nada manual.

### Por que importa
Hoje:
| Faltando | Impacto |
|---|---|
| `loan.status='OVERDUE'` | Não testa fluxo de atraso, penalidades, job noturno |
| `book_copy.status='MAINTENANCE'/'UNAVAILABLE'` | Não testa filtros de admin |
| `loan_request.status='ACCEPTED'/'REJECTED'` | Não testa visão histórica |
| `reservation.status='READY'/'FULFILLED'/'CANCELLED'/'EXPIRED'` | Não testa FIFO completo |
| `book.cover_url` populada | Mobile fica sem imagens — primeira impressão ruim |
| `thesis.pdf_url` populada | TCC fica sem PDF para abrir |
| `student.penalty_code` | Não testa bloqueio de empréstimo |
| Volume de livros | 3 livros não exercita paginação (default 20/page) |

### Tarefas
- [ ] Expandir `R__seed_demo_data.sql` para:
  - **30 livros** distribuídos em 10 gêneros — cobre paginação (2 páginas de 20)
  - Todos com `cover_url` apontando para CDN público (use `https://covers.openlibrary.org/b/isbn/{isbn}-L.jpg`)
  - **Pelo menos 1 livro com `volume=2`** (testa exibição de volume)
- [ ] **8 alunos**:
  - 6 sem penalidade, 1 com `BLOCKED_LATE` ativo, 1 com `WARNING_FIRST` vencendo amanhã
  - Distribuídos em 3 cursos diferentes (já tem 8 cursos no V5)
- [ ] **15 exemplares**:
  - 8 `AVAILABLE`, 4 `BORROWED`, 1 `MAINTENANCE`, 1 `UNAVAILABLE`, 1 `BORROWED` em atraso
- [ ] **10 empréstimos**:
  - 4 `ACTIVE` (D-10, D-5, D-2, D+1)
  - 1 `OVERDUE` (D+8)
  - 5 `COMPLETED` (alguns com `renewal_count > 0`)
- [ ] **6 solicitações**:
  - 2 `PENDING`, 2 `ACCEPTED`, 1 `REJECTED`, 1 `CANCELLED`
- [ ] **5 reservas**:
  - 2 `WAITING` (posições 1 e 2 do mesmo livro)
  - 1 `READY` (notificada)
  - 1 `FULFILLED`
  - 1 `EXPIRED`
- [ ] **3 TCCs** com `pdf_url` apontando para sample PDF público
- [ ] **3 audit_log** entries históricos
- [ ] **2 outbox_event** com status `SENT` (mostra histórico de notificações)
- [ ] Refazer `REFRESH MATERIALIZED VIEW` no final do seed

### Critério de aceitação
- Dashboard demo mostra: 4 empréstimos ativos, 1 atrasado, 2 solicitações pendentes, valor `avg_return_days` realista (não 0).
- Mobile demo: catálogo mostra capas para todos os livros.
- Web admin: paginação dos livros tem botão "Próxima página" habilitado.
- Smoke test: rodar `FlywayMigrationTest.optional_demo_seed_populates_business_data` valida contagens novas.

### Risco
- Volume maior pode estourar timeout de CI Postgres. **Mitigação:** seed roda só quando `LUMILIVRE_FLYWAY_LOCATIONS` inclui `db/seed`. CI não inclui por default.

---

## F7 — Documentação de banco

### Objetivo
Permitir que qualquer pessoa **sem Spring/Flyway** consiga (a) entender o schema, (b) criar as tabelas em outro banco, (c) migrar dados de um sistema legado.

### Por que importa
- Recrutador valida: o candidato sabe documentar para **stakeholders não-técnicos** (DBA, analista, fundadora de biblioteca)?
- Maioria de bibliotecas escolares opera com planilhas Excel ou software legado. Migração de dados é o **primeiro caso de uso real**.

### Tarefas
- [ ] **`lumilivre-api/docs/database/ERD.md`** — diagrama ER em Mermaid:
  ```mermaid
  erDiagram
      student ||--o{ loan : "borrows"
      book ||--|{ book_copy : "has"
      book_copy ||--o{ loan : "is borrowed in"
      ...
  ```
- [ ] **`lumilivre-api/docs/database/data_dictionary.md`** — para cada tabela:
  - Descrição do propósito
  - Tabela com `coluna | tipo | constraints | descrição | exemplo`
  - Invariantes de negócio (ex.: `loan.due_at >= borrowed_at` é enforced via CHECK + `LoanPolicy.validateNewLoan`)
  - Referências para o ADR que motivou o design
- [ ] **`lumilivre-api/docs/database/ddl_standalone.sql`** — DDL exportável (output de `pg_dump --schema-only`) anotado com comentários explicando por que de cada extension/index. Quem não tem Flyway só roda esse script.
- [ ] **`lumilivre-api/docs/database/migration_from_legacy.md`** — guia passo-a-passo:
  - Como importar planilha de alunos (XLSX) — aproveita `/api/import` já existente
  - Como importar livros (XLSX)
  - Como importar dump CSV de outro software (Pergamum, Biblivre, OPAC, etc.)
  - Mapeamento típico de campos legado → LumiLivre
  - Script SQL exemplo de migração com `INSERT INTO ... SELECT FROM legacy_table`
- [ ] **`lumilivre-api/docs/database/portability_notes.md`** — o que muda se quiser portar para MySQL/MariaDB/SQL Server:
  - `CITEXT` → `VARCHAR + lower()` index
  - `gen_random_uuid()` → `uuid_generate_v4()` (MySQL 8 / MariaDB)
  - `pg_trgm` GIN → equivalente em outros bancos (ou desabilitar busca avançada)
  - Materialized views → tabela + cron job de refresh
  - Lista o que o sistema **vai** perder se sair do PostgreSQL (FTS multi-idioma, busca trigram)
- [ ] Atualizar `lumilivre-api/docs/runbooks/supabase_setup.md` para mencionar alternativa "se preferir Postgres self-hosted, veja `local_environments.md`".
- [ ] **ADR-013** — registrar PostgreSQL como dependência forte (já feito implicitamente; documentar trade-off).

### Critério de aceitação
- Um DBA externo, lendo `data_dictionary.md` + `ddl_standalone.sql`, consegue subir o schema em outro Postgres em < 15 min.
- ER diagram renderiza corretamente em GitHub markdown.
- Guia de migração tem exemplo testado para 1 sistema legado (escolher um popular: Pergamum ou Biblivre).

---

## F8 — Cobertura de testes

### Objetivo
Subir o sinal de credibilidade do projeto para nível "Tier 1 ready": testar integrações externas, fluxo E2E, services não cobertos.

### Por que importa
- Hoje: 7 de 26 services testados (~27%)
- JaCoCo gate está soft (`jacoco.enforce=false`)
- Recrutador olhando o repo vê `/test/` dominado por controllers (CRUD trivial) — não vê **testes de integração de verdade**
- Tier 1 padrão: 80%+ line coverage com **testes que protegem regressões reais**

### Tarefas
- [ ] **WireMock para integrações externas** (`@Testcontainers + WireMockServer`):
  - `GoogleBooksProviderTest` — happy path, 404, 500, timeout, circuit-breaker open
  - `BrasilApiProviderTest` — idem
  - `OpenLibraryProviderTest` — idem
  - `SupabaseStorageProviderTest` — upload, signed URL, falha, retry
  - `BrazilianPostalCodeProviderTest` — CEP válido, CEP inexistente, ViaCEP fora do ar
  - `UniversalPostalCodeProviderTest` — vários países
- [ ] **E2E Testcontainers** — `LoanFullFlowIntegrationTest`:
  1. Cria aluno + livro + exemplar via API
  2. Aluno solicita o livro (`POST /api/loan-requests`)
  3. Bibliotecário aceita (`POST /api/loan-requests/{id}/accept`)
  4. Verifica `loan.status=ACTIVE`
  5. Verifica `outbox_event.status=PENDING` para email
  6. Trigger manual de `OutboxPublisherService.processPendingEvents`
  7. Verifica `outbox_event.status=SENT`
  8. Aluno devolve livro
  9. Verifica métricas (`/actuator/prometheus` tem `loans.active=0`)
  10. Verifica audit_log com 2 entries
- [ ] **Services hoje sem teste** — criar pelo menos 1 happy path + 1 erro:
  - `BookService`, `BookCopyService`, `StudentService`, `ThesisService`
  - `ImportService` (vital — XLSX parsing complexo)
  - `ReportService` (PDF generation)
  - `RecommendationService`
  - `BusinessMetricsService`
  - `AppUserService`
  - `DueDateNotificationJob`
- [ ] **Security tests**:
  - `AuthRateLimitFilterTest` — Bucket4j 5 req/10min
  - `StudentAuthorizationServiceTest` — matriz IDOR (admin acessa qualquer; aluno só ele mesmo)
  - `AuditAspectTest` — já tem 1; expandir para cobrir `FAILURE` e `DENIED`
- [ ] **HomeController** — adicionar `HomeControllerTest` para fechar 19/19
- [ ] **Architecture tests** com ArchUnit:
  - Domain (`domain/policy/**`) não importa Spring nem JPA
  - Controllers não chamam Repositories diretamente
  - DTOs não importam entities
  - `service.infra.**` é o único lugar onde RestTemplate/HttpClient pode aparecer
- [ ] **Flip do JaCoCo gate** em `pom.xml:26`: `<jacoco.enforce>true</jacoco.enforce>` quando coverage atingir o `jacoco.line.minimum=0.70`.
- [ ] **`lumilivre-api/.github/workflows/api.yml`**: rodar `./mvnw verify -Djacoco.enforce=true` em PR — bloqueia merge se cobertura cair.

### Critério de aceitação
- Line coverage ≥ 70% (atual provavelmente ~35-40%)
- Branch coverage em `domain/policy/` ≥ 95% (já configurado no pom)
- E2E loan-flow passa em CI
- ArchUnit gate verde
- Tempo total de CI ≤ 8 minutos (paralelizar Testcontainers via Surefire forks)

---

## F9 — Polish open-source

### Objetivo
Fechar a `Definition of Done` do `opensource.md` §7.

### Tarefas (cross-repo)

#### Meta (`lumilivre/`)
- [ ] `LICENSE` (MIT) na raiz
- [ ] `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1)
- [ ] `CONTRIBUTING.md` mestre com workflow de submodules
- [ ] `ROADMAP.md` derivado do `IDIOMES.md` (versão pública resumida)
- [ ] `CITATION.cff` para citação acadêmica (TCC)
- [ ] `.github/ISSUE_TEMPLATE/bug_report.yml`
- [ ] `.github/ISSUE_TEMPLATE/feature_request.yml`
- [ ] `.github/PULL_REQUEST_TEMPLATE.md`
- [ ] `.github/FUNDING.yml` (opcional — GitHub Sponsors)
- [ ] `SECURITY.md` já existe; verificar se tem `Reporting a Vulnerability` section com email/GitHub Security Advisory

#### Cada sub-repo
- [ ] `CHANGELOG.md` (Keep a Changelog format) com entrada `v0.1.0`
- [ ] Tag `v0.1.0` após merge final
- [ ] Templates de issue/PR específicos do sub-projeto
- [ ] `.github/dependabot.yml` cobrindo `maven`/`npm`/`pub` + GitHub Actions
- [ ] Branch protection no `main`/`master`: require PR + 1 approval + CI verde

#### OpenAPI (`lumilivre-api/`)
- [ ] Fechar §5 Etapa A do `IDIOMES.md`: exemplos PT-BR/EN-US, erros, roles em **todos** os endpoints
- [ ] Schemas em `OpenApiConfig.java` com `description` traduzido
- [ ] `/swagger-ui` renderiza igual em ambos locales
- [ ] Gerar `openapi.json` em CI artifact (consumido por `lumilivre-web` orval gen)

#### Landing (`lumilivre-web/`)
- [ ] FASE O3 do `opensource.md` §4: implementar landing pública em `/` movendo painel para `/admin/*`
- [ ] SEO via `react-helmet-async`
- [ ] Sem dependência de JWT/AuthContext
- [ ] Testes e2e Playwright cobrindo `/`

### Critério de aceitação
- `opensource.md` §7 todo marcado.
- Tag `v0.1.0` em cada um dos 4 repos.
- `gh release create` com release notes geradas do CHANGELOG.

---

## F10 — Diferenciais "wow" para recrutador Tier 1

### Objetivo
Itens **não bloqueantes** mas que diferenciam o projeto em uma página de portfólio. Implementar **pelo menos 3 dos 7** abaixo.

### Por que importa
A diferença entre "candidato cumpriu requisitos" e "candidato é senior staff" mora aqui. Cada item abaixo é um item de discussão em entrevista técnica.

### Opções

#### F10.1 — Observabilidade nível produção (recomendado: faça)
- [ ] **OpenTelemetry traces** (Spring Boot 3 tem auto-config): exportar via OTLP para Jaeger/Tempo local
- [ ] `docker-compose.observability.yml` com Prometheus + Grafana + Loki + Tempo
- [ ] Grafana dashboard versionado em `docs/observability/grafana/` (já existe esqueleto)
- [ ] **SLO formal** em `docs/observability/SLOs.md`: API p99 < 500ms, error rate < 1%, uptime 99.5%
- [ ] Prometheus alerts em `docs/observability/alerts.yaml` (já existe esqueleto)

#### F10.2 — Performance testing (recomendado: faça)
- [ ] **k6 scripts** em `lumilivre-api/perf/`:
  - `login.js` — sustained 50 RPS no `/api/auth/login`
  - `book-search.js` — 100 RPS no `/api/books/search?q=...`
  - `loan-flow.js` — fluxo completo full-stack
- [ ] CI workflow `perf.yml` que roda nightly + comenta resultado em PR
- [ ] Baseline documentado em `docs/performance/baseline.md`

#### F10.3 — Native image com GraalVM
- [ ] `Dockerfile.native` com `paketobuildpacks/builder:tiny`
- [ ] Spring Boot 3 já tem suporte AOT. Testar `./mvnw -Pnative spring-boot:build-image`
- [ ] Comparar startup time: JVM ~12s vs native ~50ms — ótimo número para README
- [ ] Documentar trade-off (build longo, reflection hints)

#### F10.4 — Kubernetes manifests
- [ ] `lumilivre-api/k8s/` com:
  - `deployment.yaml` (3 réplicas)
  - `service.yaml`
  - `hpa.yaml` (autoscaling por CPU + custom metric `loans_active`)
  - `ingress.yaml` (cert-manager + TLS)
  - `configmap.yaml` + `secret.yaml` (SOPS-encrypted)
- [ ] Helm chart em `lumilivre-api/charts/lumilivre/`
- [ ] Documentar em `docs/deploy/kubernetes.md`

#### F10.5 — Feature flags reais
- [ ] **GrowthBook** ou **Unleash** integrado em vez do toggle hardcoded `app.api.enabled`
- [ ] Cada nova feature do roadmap entra atrás de flag
- [ ] Demonstra **trunk-based development** maduro

#### F10.6 — Contract testing
- [ ] **Spring Cloud Contract** ou **Pact** entre API e web/app
- [ ] CI quebra se mudança em DTO quebrar contrato dos clientes

#### F10.7 — Domain events publicados externamente
- [ ] Estender `OutboxPublisherService` para publicar em **Kafka** (além de SMTP)
- [ ] Tópico `lumilivre.loans.events` consumível por sistemas externos (Data Warehouse, BI)
- [ ] `docker-compose.kafka.yml` opcional
- [ ] Documenta **Event-Driven Architecture** real

### Recomendação concreta para 0.1.0
Implementar **F10.1 (OTEL) + F10.2 (k6) + F10.3 (GraalVM)**:
- Custo: ~3 dias
- Impacto no README: enorme. Coloca esses 3 badges no topo:
  ```markdown
  ![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-traced-purple)
  ![k6 Perf](https://img.shields.io/badge/k6-baselined-orange)
  ![GraalVM Native](https://img.shields.io/badge/GraalVM-50ms_startup-blue)
  ```

---

## Outras considerações estruturais

### Submodules vs polyrepo
O `MEMORY.md` do meta registra a topologia atual: meta + 3 sub-repos com `.git` próprio. Validar se essa estratégia é mantida no público. Considerações:

| Opção | Pró | Contra |
|---|---|---|
| **Manter polyrepo** (status quo) | Cada repo tem own CI, own releases | Contribuir exige clone+setup de 4 repos |
| **Migrar para monorepo** com pnpm workspaces / Nx / Bazel | 1 PR cobre mudanças cross-stack | Quebra histórico, exige reescrever CI |
| **Manter polyrepo + git submodules** explícitos no meta | Clone único `--recurse-submodules` | Submodules são notoriamente confusos |

**Recomendação:** manter polyrepo + adicionar `git submodule` config no meta para o clone único. Documentar no `CONTRIBUTING.md`.

### Versionamento
- Hoje: `pom.xml:17` declara `1.0.0-PROD`. Confuso.
- Migrar para **Semantic Versioning** alinhado entre os 4 repos: `0.1.0`, `0.2.0`, `1.0.0` quando API estável.
- Tags Git em todos os repos no mesmo bump.

### Hospedagem da demo pública
- Hoje API em Render + Supabase. Custo controlado mas latência ruim.
- Avaliar Fly.io (free tier suficiente para demo) + Neon (Postgres serverless).
- Documentar **como hospedar gratuitamente** em `docs/deploy/free-tier.md`.

---

## Ordem de execução recomendada (8 semanas full-time)

```
Semana 1: F0 + F1                          → DX local pronto, hotfixes aplicados
Semana 2: F7 + F9 (parcial)                → Documentação banco + LICENSE/CHANGELOG
Semana 3: F2 (Storage)                     → Primeira modularização entregue
Semana 4: F3 (Book Metadata)               → Segunda modularização
Semana 5: F4 (Postal) + F5 (i18n services) → Terceira modularização + i18n fechado
Semana 6: F6 (Seed) + F9 (resto)           → Demo completa + polish OSS
Semana 7: F8 (Testes)                      → Cobertura ≥ 70%
Semana 8: F10 (3 itens)                    → Diferenciais "wow"

Tag v0.1.0 → Anúncio público → Coleta feedback → v0.2.0 backlog
```

Caminho mais curto (4 semanas) — pular F2/F3/F4, manter modularização para v0.2.0:
```
Semana 1: F0 + F1
Semana 2: F5 + F6 + F7
Semana 3: F8 (parcial — 60% coverage)
Semana 4: F9 + F10.1 (OTEL)
→ v0.1.0 público em 4 semanas, modularização vira o headline do v0.2.0
```

---

## Definition of Done — v0.1.0 público

Checklist final que materializa as fases:

### Bloqueantes
- [ ] F0 — bugs de seed corrigidos, `lumilivre-key.pem` removido
- [ ] F1 — `just setup` funcional, README atualizado
- [ ] F5 — i18n fechado (services + emails + PDFs)
- [ ] F6 — seed exercita 100% da UI
- [ ] F7 — schema documentado (ER + dicionário + DDL standalone + migração)
- [ ] F9 — LICENSE, CHANGELOG, ROADMAP, templates, CONTRIBUTING

### Recomendados
- [ ] F2 — Storage modularizado
- [ ] F3 — Book Metadata modularizado
- [ ] F4 — Postal Code modularizado
- [ ] F8 — Cobertura ≥ 70%
- [ ] F10 — pelo menos OTEL + k6

### Estado final esperado
- `docker compose up` → API + DB + Redis + Mail capture saudáveis em < 90s
- 3 contas demo funcionando (admin / librarian / student)
- UI consegue exercitar todos os fluxos críticos sem cadastro manual
- API respondendo em PT-BR e EN-US por `Accept-Language`
- Schema documentado para uso fora do Spring/Flyway
- Modularização permite trocar Supabase/Google Books/ViaCEP sem mexer em service
- Testes E2E cobrem fluxo de loan ponta-a-ponta
- README do meta tem badges de cobertura, perf baseline, e GraalVM startup time

---

## Apêndice — Mapa de referências cruzadas

| Tópico | Arquivos relevantes |
|---|---|
| i18n infrastructure | `lumilivre-api/src/main/java/br/com/lumilivre/api/config/I18nConfig.java`, `MessageResolver.java` |
| i18n bundles | `lumilivre-api/src/main/resources/i18n/*/messages_*.properties` |
| Storage acoplamento | `lumilivre-api/src/main/java/br/com/lumilivre/api/service/infra/SupabaseStorageService.java` + 3 consumers |
| Book metadata acoplamento | `BookService.java:263-323`, `GoogleBooksService.java`, `BrasilApiService.java` |
| Postal code acoplamento | `CepService.java`, `MetadataController.java:88-111`, `StudentService.java:255-275` |
| Schema migrations | `lumilivre-api/src/main/resources/db/migration/V1..V5` |
| Seed demo | `lumilivre-api/src/main/resources/db/seed/R__seed_demo_data.sql` |
| Plano i18n | `IDIOMES.md` §5 Etapa A |
| Plano OSS | `opensource.md` §2 e §7 |
| ADRs vigentes | `docs/adr/ADR-001..010` |
| Threat model | `SECURITY.md` |

---

> Este documento é vivo. À medida que cada fase fecha, marcar checkbox + atualizar % na tabela do Sumário Executivo. Considerar mover para `ROADMAP.md` (público) o que for de interesse externo, mantendo este `FINISH.md` como plano interno detalhado até o release.
