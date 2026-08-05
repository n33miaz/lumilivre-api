<div align="center">
  <!-- Banner -->
  <a href="https://n33miaz.github.io/n33miaz-links/#lumitcc"><img width="100%" src="https://github-stats-api-rfi2.onrender.com/api/banner?title=LumiLivre&subtitle=Library%20Management%20System&tag=(TCC)%20Bachelor%27s%20Thesis&title_color=762075&text_color=c9d1d9&v=1" /></a>

  <!-- Pins-->
  <a href="https://github.com/n33miaz/lumilivre-web"><img src="https://github-stats-api-rfi2.onrender.com/api/pin?username=n33miaz&repo=lumilivre-web&custom_title=WebSite&bg_color=0d1117&title_color=762075&text_color=c9d1d9&icon_color=762075&hide_border=true&min_width=270&show_description=false&v=1" /></a>
  <a href="https://github.com/n33miaz/lumilivre-app"><img src="https://github-stats-api-rfi2.onrender.com/api/pin?username=n33miaz&repo=lumilivre-app&custom_title=Application&bg_color=0d1117&title_color=762075&text_color=c9d1d9&icon_color=762075&hide_border=true&min_width=270&show_description=false&v=1" /></a>
  <a href="https://github.com/n33miaz/lumilivre-api"><img src="https://github-stats-api-rfi2.onrender.com/api/pin?username=n33miaz&repo=lumilivre-api&custom_title=API%20Restfull&bg_color=0d1117&title_color=762075&text_color=c9d1d9&icon_color=762075&hide_border=true&min_width=270&show_description=false&v=1" /></a>
</div>

<br/>

<div align="center">

![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-762075?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-red?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?style=flat-square&logo=docker)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-blue?style=flat-square&logo=githubactions)
[![API Docs](https://img.shields.io/badge/docs-Swagger-purple?style=flat-square)](https://lumilivre-api.onrender.com/docs)

</div>

<br/>

<div align="center">
  <h1>LumiLivre API</h1>
  <p><em>Núcleo REST do ecossistema LumiLivre — regras de negócio, persistência e segurança.</em></p>
</div>

A **LumiLivre API** centraliza a lógica de uma biblioteca escolar: catálogo e
exemplares, empréstimos com renovação e multa, fila de reservas FIFO,
solicitações do leitor, avisos do mural, relatórios e dashboard. Serve o painel
administrativo (React) e o app do leitor (Flutter) pela mesma superfície REST.

São 23 controllers, 6 migrations versionadas e 65 classes de teste. A
documentação interativa fica em `/docs`.

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem / runtime | Java 17 (Eclipse Temurin) |
| Framework | Spring Boot 3.2.5 — Web MVC, Data JPA, Security, Cache, Mail, Actuator, AOP |
| Persistência | PostgreSQL 16 + Hibernate + Flyway |
| Cache | `ConcurrentMap` por padrão; Redis opcional via Spring Data Redis |
| Observabilidade | Logback JSON, Micrometer + Prometheus, `correlationId` no MDC |
| Resiliência | Resilience4j — retry, circuit breaker, time limiter, fallback |
| Segurança | Spring Security, JWT (`jjwt`), BCrypt, Bucket4j |
| Assíncrono | Outbox pattern com publisher `@Scheduled` |
| Docs | springdoc-openapi 2.5.0 |
| Relatórios | OpenPDF |
| Planilhas | Apache POI 5.3 (XLSX) |
| Testes | JUnit 5, Mockito, ArchUnit, Testcontainers, WireMock |

## Subindo em um comando

O caminho mais curto é o `docker-compose.yml` do repositório de orquestração
[`lumilivre`](https://github.com/n33miaz/lumilivre), que sobe PostgreSQL, esta
API e o painel web já migrados e populados:

```powershell
docker compose up -d --build
```

Só a API, contra um PostgreSQL que você já tenha:

```powershell
docker build -t lumilivre-api .
docker run -p 8080:8080 --env-file .env lumilivre-api
```

| Recurso | Endereço |
|---------|----------|
| Swagger UI | http://localhost:8080/docs |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

## Desenvolvendo local

Requer Java 17 e um PostgreSQL 16 alcançável. Copie `.env.example` para `.env`,
preencha as variáveis e rode:

```powershell
.\mvnw.cmd spring-boot:run     # sobe em http://localhost:8080
.\mvnw.cmd test                # suíte de testes
.\mvnw.cmd verify              # testes + gate de cobertura JaCoCo
```

O PostgreSQL precisa das extensões `pgcrypto`, `citext`, `pg_trgm` e `unaccent`
— `docker/postgres/init.sql` as cria. Usando o compose do repositório de
orquestração, isso já vem feito.

Há também um `justfile` com atalhos (`just setup`, `just api`, `just migrate`)
para quem tem o [just](https://just.systems) instalado.

## Configuração

Nenhum segredo é versionado — tudo entra por variável de ambiente. Estas nove não
têm default e a aplicação falha na subida sem elas:

| Variável | Uso |
|----------|-----|
| `LUMILIVRE_DB_URL` · `_DB_USER` · `_DB_PASSWORD` | Conexão PostgreSQL |
| `LUMILIVRE_JWT_SECRET` | Assinatura HS256 — mínimo 32 caracteres |
| `LUMILIVRE_MAIL_USERNAME` · `_MAIL_PASSWORD` | SMTP das notificações |
| `LUMILIVRE_SUPABASE_URL` · `_SUPABASE_KEY` · `_SUPABASE_SERVICE_ROLE_KEY` | Storage remoto |

As opcionais que mais importam:

| Variável | Default | Uso |
|----------|---------|-----|
| `PORT` | `8080` | Porta HTTP (Render/Heroku injetam) |
| `LUMILIVRE_CORS_ORIGINS` | `localhost:5173,localhost:8080` | Origens liberadas |
| `LUMILIVRE_STORAGE_PROVIDER` | `supabase` | `local` grava em disco, sem Supabase |
| `LUMILIVRE_FLYWAY_LOCATIONS` | `classpath:db/migration` | Inclua `classpath:db/seed` para popular a demo |
| `LUMILIVRE_CACHE_TYPE` · `LUMILIVRE_REDIS_URL` | `simple` | Troque para `redis` em produção |
| `LUMILIVRE_API_ENABLED` | `true` | `false` bloqueia todo `/api/**` (rollback) |
| `LUMILIVRE_SCHEDULING_ENABLED` | `true` | Desliga os jobs agendados |

`.env.example` traz o conjunto completo comentado.

## Banco de dados

Seis migrations em `src/main/resources/db/migration`, aplicadas pelo Flyway na
subida e agrupadas por assunto:

| Migration | Conteúdo |
|-----------|----------|
| `V1__core_schema` | Extensões, helpers, tabelas de referência e todo o domínio (leitores, usuários, acervo, exemplares, empréstimos, solicitações, reservas, mural) |
| `V2__infrastructure_tables` | Outbox de e-mail, auditoria de negócio e trilha de acessos |
| `V3__configuration_tables` | Configurações da biblioteca e versão mínima do app |
| `V4__indexes_and_search` | Índices de FK, de filtro e GIN/trigram para busca |
| `V5__materialized_views` | Views do dashboard |
| `V6__reference_data` | Cursos, gêneros, módulos, turnos e Dewey |

Destaques do schema:

- **Row Level Security** habilitada e negando por padrão nas tabelas de domínio.
- **Views materializadas** `mv_dashboard_stats`, `mv_top_books` e
  `mv_loans_by_month` alimentam o dashboard sem varrer as tabelas quentes.
- **Busca textual** com `pg_trgm` e `unaccent`, tolerante a acento e digitação.
- Tabelas append-only de infraestrutura (`audit_log`, `access_log`,
  `outbox_event`) com PK `BIGINT IDENTITY`.

O `ddl-auto` fica em `validate`: o schema é responsabilidade do Flyway, nunca do
Hibernate.

> Uma migration versionada já aplicada é **imutável**, inclusive nos comentários:
> o Flyway calcula o checksum sobre o arquivo inteiro, então qualquer byte
> alterado faz a subida falhar com *checksum mismatch* em todo banco que já a
> rodou. Corrija sempre com uma nova migration. Só o seed `R__` pode ser editado,
> porque migrations repetíveis são reaplicadas quando o checksum muda.

### Seed de demonstração

`src/main/resources/db/seed/R__seed_demo_data.sql` popula 50 leitores, 100
livros, 150 exemplares, 60 empréstimos em todos os estados, filas de reserva,
avisos e trilha de auditoria — o suficiente para nenhuma tela ficar vazia.

É **opt-in**: fica fora do caminho padrão porque cria credenciais fixas e, sendo
repetível, reescreve a senha do admin a cada migração. Para usar em dev:

```powershell
$env:LUMILIVRE_FLYWAY_LOCATIONS = "classpath:db/migration,classpath:db/seed"
```

Credenciais criadas pelo seed (o login aceita e-mail **ou** matrícula):

| Usuário | Senha | Papel |
|---------|-------|-------|
| `admin` | `admin` | ADMIN |
| `librarian` | `librarian` | LIBRARIAN |
| `2024001` | `2024001` | READER — entra com troca de senha obrigatória |

## Documentação da API

Swagger UI em `/docs`, spec em `/v3/api-docs`. Três grupos: `api-pt-br`,
`api-en-us` e `system`. Todos os controllers têm `@Tag` e toda operação tem
`@Operation` com `operationId`, título e descrição nos dois idiomas —
`scripts/check-openapi-annotations.sh` falha o CI se alguma faltar.

As respostas honram `Accept-Language` (`pt-BR` ou `en-US`) e devolvem
`Content-Language` e `X-Correlation-Id`. Os textos vivem em
`src/main/resources/i18n/`, e `scripts/check-i18n-coverage.sh` garante paridade
de chaves entre os locales.

## Regras de negócio

As decisões de domínio ficam em classes puras, sem Spring nem JPA, em
`domain/policy/`: `LoanPolicy`, `BookAvailabilityPolicy`, `PenaltyPolicy`,
`RequestApprovalPolicy` e `ReservationPolicy`. Violação vira HTTP 422 com
mensagem localizada.

Penalidade por atraso, em dias de suspensão:

| Atraso | Suspensão |
|--------|-----------|
| 0–1 dia | nenhuma |
| 2–5 dias | 7 dias |
| 6–7 dias | 15 dias |
| 8–90 dias | 30 dias |
| acima de 90 dias | bloqueio até resolução manual |

## Tarefas agendadas

| Job | Quando | O que faz |
|-----|--------|-----------|
| `OverdueMarkerJob` | diário | Marca vencidos como atrasados e aplica a penalidade |
| `DueDateNotificationJob` | diário | Avisa em D-3, D-1, no dia e no atraso |
| `OutboxPublisherService` | a cada 30 s | Entrega os e-mails pendentes do outbox |

O outbox grava o evento na mesma transação da operação de domínio, então uma
falha de SMTP nunca desfaz um empréstimo. Desligue tudo com
`LUMILIVRE_SCHEDULING_ENABLED=false`.

## Segurança

- **JWT HS256** validado a cada requisição; sessão stateless.
- **Autorização em duas camadas** — regras de URL no `SecurityConfig` mais
  `@PreAuthorize` por método, para que uma falha em uma não abra a outra.
- **Prevenção de IDOR** — `@CanAccessReader` e `@CanAccessLoan` garantem que um
  leitor só alcança os próprios dados.
- **Rate limit** (Bucket4j) em `/api/auth/login`, `/api/auth/forgot-password` e
  `/api/auth/reset-password`, com trava adicional por conta.
- **Troca de senha obrigatória** no primeiro acesso, imposta no servidor por um
  filtro que bloqueia escritas até a troca acontecer.
- **Upload validado por magic bytes**, não pela extensão nem pelo `Content-Type`.
- **Auditoria** — escritas anotadas com `@Auditable` gravam ator, papel, alvo e
  resultado; tentativas negadas vão para `access_log`.
- **Teto de paginação** global de 100 itens por página.

## Observabilidade

`/actuator/health` é público (usado pelo healthcheck do container);
`/actuator/info` e `/actuator/prometheus` exigem ADMIN. As métricas de domínio
expostas são `loans.active`, `loans.overdue`, `requests.pending` e
`returns.avg_days`.

Todo request recebe um `X-Correlation-Id`, propagado pelo MDC e presente em cada
linha de log JSON.

## Integrações externas

Metadados de livro por ISBN vêm de uma cadeia configurável
(`LUMILIVRE_BOOK_METADATA_PROVIDERS`): Google Books, BrasilAPI e Open Library,
consultados em ordem até um responder. CEP usa a mesma estratégia. Todas as
chamadas passam por circuit breaker, retry e timeout do Resilience4j com
fallback — indisponibilidade externa degrada a funcionalidade, não a API.

## Testes e CI

```powershell
.\mvnw.cmd test      # unitários + ArchUnit
.\mvnw.cmd verify    # inclui o gate de cobertura JaCoCo
```

A suíte cobre serviços, controllers, políticas de domínio, filtros de segurança,
regras de arquitetura (ArchUnit impede controller conversar direto com
repository) e um fluxo completo de empréstimo em Testcontainers contra
PostgreSQL real.

O workflow `.github/workflows/api.yml` roda, em ordem: Gitleaks, paridade de
i18n, cobertura de anotações OpenAPI, `mvnw verify` com o gate de cobertura e o
empacotamento.

## Deploy

`Dockerfile` multi-stage: build com Maven, runtime em JRE 17 slim, usuário
não-root, `JAVA_OPTS` dimensionado para container pequeno
(`-XX:MaxRAMPercentage=75`) e healthcheck em `/actuator/health`. A porta vem de
`PORT` quando o host a injeta.

## Licença

**Proprietário — todos os direitos reservados.** Veja [`LICENSE`](LICENSE). O
código é publicado para leitura, estudo e avaliação técnica; qualquer uso, cópia
ou execução em produção requer licença comercial — **ncormino@gmail.com**.

<br/>

<div align="center">
  <sub>LumiLivre © 2026 — Gestão de bibliotecas escolares · Todos os direitos reservados.</sub>
</div>
