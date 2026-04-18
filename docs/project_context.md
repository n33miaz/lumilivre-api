# LumiLivre API - Project Context

## Objetivo

`lumilivre-api` e o backend central do ecossistema LumiLivre. Ele concentra regras de negocio, persistencia, autenticacao, integracoes externas, uploads, importacao de planilhas e geracao de relatorios para o painel web administrativo e para o aplicativo mobile dos alunos.

## Stack Tecnologica

- Linguagem: Java 17.
- Framework principal: Spring Boot 3.2.5.
- Build: Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Web/API: Spring Web MVC, controllers REST, JSON via Jackson.
- Persistencia: Spring Data JPA, Hibernate, PostgreSQL Driver 42.7.3.
- Banco: PostgreSQL hospedado em Supabase, com `ddl-auto=validate`.
- Seguranca: Spring Security, JWT via `jjwt` 0.11.5, BCrypt.
- Documentacao de API: Springdoc OpenAPI 2.5.0 e Swagger UI.
- Cache: Spring Cache com `ConcurrentMapCacheManager`.
- Email: Spring Boot Starter Mail via SMTP.
- Relatorios: OpenPDF.
- Excel/importacao: Apache POI 5.3.0.
- Utilitarios: Lombok, Apache Commons Lang.
- Deploy: Dockerfile multi-stage com Maven e Eclipse Temurin 17 JRE Alpine; `Procfile` para execucao Java.

## Arquitetura Observada

O projeto usa arquitetura em camadas no estilo MVC/Service/Repository:

- `controller`: borda HTTP REST, validacao inicial de parametros e contrato de endpoints.
- `service`: camada de aplicacao e dominio, onde ficam as regras de emprestimo, solicitacao, livro, aluno, usuario, TCC, importacao e relatorios.
- `repository`: acesso a dados via Spring Data JPA, incluindo queries JPQL e nativas otimizadas.
- `model`: entidades JPA que representam tabelas e relacionamentos.
- `dto`: contratos de entrada/saida, separados por dominio.
- `security`: filtro JWT, utilitario de token e adaptadores `UserDetails`.
- `config`: seguranca, CORS, OpenAPI, cache e beans de infraestrutura.
- `service/infra`: adaptadores para servicos externos como Google Books, BrasilAPI, ViaCEP, Supabase Storage e email.
- `exception`: excecoes de dominio e tratador global.
- `utils`: utilitarios como leitura de Excel.

Nao ha Clean Architecture ou Hexagonal Architecture completa, porque controllers, services e repositories dependem diretamente de frameworks Spring/JPA. A separacao atual e adequada para um sistema monolitico modular, mas uma evolucao para portas/adaptadores exigiria interfaces de repositorio/infra no dominio e isolamento maior das entidades de persistencia.

## Modulos Principais

- Autenticacao e usuarios: `AuthController`, `AuthService`, `UsuarioController`, `UsuarioService`, `JwtUtil`, `JwtAuthenticationFilter`.
- Alunos: cadastro, edicao, exclusao, reset de senha, foto de perfil, enriquecimento de endereco por CEP e ranking.
- Livros e catalogo: cadastro, atualizacao, consulta por ISBN, agrupamento, filtros avancados, catalogo mobile por genero e detalhes de disponibilidade.
- Exemplares: controle fisico por tombo, status, localizacao, vinculo com livro e sincronizacao de quantidade.
- Emprestimos: criacao, atualizacao, conclusao, exclusao, historico do aluno, dashboard, atrasos e ranking.
- Solicitacoes de emprestimo: criacao por tombo ou por livro, processamento por bibliotecario, historico e dashboard.
- TCCs: cadastro com JSON multipart, PDF, foto, filtro e controle de ativo.
- Relatorios: geracao de PDF para emprestimos, alunos, livros, exemplares e estatisticas.
- Importacao: carga `.xlsx` de alunos, livros e exemplares com validacao, deduplicacao e processamento em lotes.
- Dados auxiliares: cursos, turnos, modulos, generos, CDDs e enums.

## Autenticacao e Autorizacao

- Login em `/auth/login` aceita `user` como email ou matricula.
- Alunos usam matricula como identificador; admins e bibliotecarios usam email.
- A senha e validada com BCrypt.
- O JWT carrega o subject como email do usuario e um claim `roles`.
- Clientes enviam `Authorization: Bearer <token>`.
- `JwtAuthenticationFilter` ignora `/auth/**`, extrai o token, valida expiracao e popula o `SecurityContext`.
- `@EnableMethodSecurity` habilita `@PreAuthorize` nas controllers.
- Perfis observados: `ADMIN`, `BIBLIOTECARIO`, `ALUNO`.
- `SecurityConfig` aplica allowlist explicita (`/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/apresentacao/**`) e exige autenticacao em todos os demais endpoints (`anyRequest().authenticated()`).
- `@CanAccessStudent` + `StudentAuthorizationService` bloqueiam acesso cruzado entre alunos; admin/bibliotecario nao sao impactados.
- `AuthRateLimitFilter` (Bucket4j) aplica quota em `/auth/login` e `/auth/esqueci-senha` (HTTP 429 apos N falhas).
- Senha inicial: o login retorna `isInitialPassword=true` quando a senha do aluno e igual a matricula ou a senha administrativa e igual ao email.
- Recuperacao de senha: token UUID com validade de 30 minutos, email com link para `https://www.lumilivre.com.br/mudar-senha?token=...`, troca de senha e remocao do token apos uso.

## Regras de Negocio Relevantes

- Aluno:
  - Matricula e unica.
  - CPF e unico quando informado.
  - Email deve ser unico em `usuario`.
  - Curso, turno e modulo devem existir.
  - Quando aluno tem email, um usuario `ALUNO` e criado com senha inicial igual a matricula.
  - Reset de senha de aluno redefine a senha para a matricula.
  - CEP com 8 digitos pode preencher endereco via ViaCEP.

- Livro:
  - ISBN e unico quando informado.
  - Se ISBN existir, a API tenta preencher metadados via Google Books e usa BrasilAPI como fallback.
  - Titulo, editora e autor sao obrigatorios.
  - Data de lancamento nao pode ser futura.
  - CDD informado precisa existir.
  - A associacao de generos e limitada a ate 3 generos encontrados.
  - Avaliacao padrao e `4.6` quando a integracao nao retorna valor.

- Exemplar:
  - Tombo e obrigatorio e unico.
  - Livro vinculado e obrigatorio.
  - Status valido: `DISPONIVEL`, `EMPRESTADO`, `INDISPONIVEL`, `EM_MANUTENCAO`.
  - Exemplar com emprestimo `ATIVO` ou `ATRASADO` nao pode ser excluido.
  - Quantidade do livro e recalculada a partir dos exemplares.

- Emprestimo:
  - Datas de emprestimo e devolucao sao obrigatorias.
  - Devolucao nao pode ser anterior ao emprestimo.
  - Aluno com penalidade ativa nao pode receber novo emprestimo.
  - Penalidade expirada e removida durante a tentativa de novo emprestimo.
  - Limite observado: 3 emprestimos ativos por aluno.
  - Exemplar precisa estar `DISPONIVEL`.
  - Ao cadastrar emprestimo, status do emprestimo vira `ATIVO`, exemplar vira `EMPRESTADO` e contador do aluno e incrementado.
  - Emprestimo concluido nao pode ser alterado ou concluido novamente.
  - Ao concluir, exemplar volta para `DISPONIVEL`.
  - Se houver atraso, a penalidade e calculada por dias de atraso e aplicada ao aluno por 7 dias quando for mais grave que a atual.
  - Excluir emprestimo decrementa contador do aluno e libera exemplar quando o emprestimo estava ativo ou atrasado.

- Penalidades:
  - `<= 1` dia: `REGISTRO`.
  - `2..5` dias: `ADVERTENCIA`.
  - `6..7` dias: `SUSPENSAO`.
  - `8..90` dias: `BLOQUEIO`.
  - `> 90` dias: `BANIMENTO`.

- Solicitacao:
  - Status inicial e `PENDENTE`.
  - Aluno com penalidade ativa nao pode solicitar.
  - Limite de solicitacao considera emprestimos `ATIVO` + `ATRASADO` e bloqueia quando soma >= 3.
  - Solicitacao por livro escolhe o primeiro exemplar disponivel.
  - Aceitar solicitacao cria emprestimo com prazo de 14 dias e marca a solicitacao como `ACEITA`.
  - Rejeitar solicitacao marca status `REJEITADA`.

- TCC:
  - Titulo, alunos e curso sao obrigatorios.
  - Curso deve existir.
  - PDF de TCC aceita apenas `application/pdf`.
  - Foto/capa e PDF sao enviados para Supabase Storage.

- Importacao:
  - Arquivo deve ser `.xlsx`.
  - Tipos aceitos: `aluno`, `livro`, `exemplar`.
  - Processamento em lotes de 50.
  - Alunos importados geram usuario `ALUNO` com senha inicial igual a matricula.
  - Livros importados exigem CDD valido.
  - Exemplares importados atualizam a contagem dos livros.

## Dependencias Criticas e Integracoes

- Supabase PostgreSQL: fonte persistente de dados relacionais.
- Supabase Storage: capas, fotos de aluno e PDFs de TCC.
- Google Books API: enriquecimento de livros por ISBN, titulo e autor.
- BrasilAPI ISBN: fallback para metadados de livros.
- ViaCEP: preenchimento de endereco de aluno por CEP.
- SMTP Gmail: envio de senha inicial, recuperacao de senha, notificacoes de emprestimo e solicitacao.
- Swagger/OpenAPI: documentacao operacional dos endpoints.

Observacao de seguranca: configuracoes sensiveis aparecem em `application.properties`. Para ambiente profissional, mover credenciais de banco, JWT, SMTP e Supabase para variaveis de ambiente ou cofre de segredos.

## Estrutura de Pastas

```text
src/main/java/br/com/lumilivre
  LumilivreApplication.java
  api/
    config/
    controller/
      system/
    dto/
    enums/
    exception/
    model/
    repository/
    security/
    service/
      infra/
    utils/
src/main/resources/
  application.properties
  certificates/
src/test/java/
```

## Comandos Essenciais

```powershell
# instalar/compilar dependencias
.\mvnw.cmd clean install

# executar localmente
.\mvnw.cmd spring-boot:run

# executar testes
.\mvnw.cmd test

# gerar artefato
.\mvnw.cmd clean package

# build Docker
docker build -t lumilivre-api .

# executar container
docker run -p 8080:8080 lumilivre-api
```

## Qualidade, Escalabilidade e Pontos de Atencao

- O projeto tem boa coesao por dominio em controllers, services e DTOs.
- O uso de DTOs evita expor diretamente entidades em muitos fluxos, embora ainda existam retornos de entidades em algumas rotas.
- Repositories encapsulam queries especificas e evitam logica SQL nas controllers.
- A camada de service concentra regras de negocio, o que facilita testes unitarios futuros.
- `application.properties` agora consome variaveis `${ENV}` e `application-example.properties` serve como template.
- Injecao por construtor padronizada via `@RequiredArgsConstructor` em services, controllers e filtros.
- Cache distribuido via Redis (`CacheConfig`) com fallback `ConcurrentMap` para perfil dev.
- Resilience4j aplicado em integracoes externas (Google Books, BrasilAPI, Supabase Storage) com retry/circuit-breaker/timeout/fallback.
- Outbox Pattern desacopla envio de email da transacao principal e cobre retry at-least-once.
- Job `@Scheduled` diario sincroniza `ATIVO -> ATRASADO` e alimenta notificacoes D-3/D-1/D0/atraso.

## Evolucao Arquitetural Recente

- **Seguranca**: `SecurityConfig` com allowlist explicita + `anyRequest().authenticated()`. Ownership por matricula via `@CanAccessStudent` e `StudentAuthorizationService`. Rate-limit em `/auth/**` com Bucket4j. CORS por perfil via env.
- **Migrations**: Flyway habilitado (`V1__baseline.sql`, `V2__outbox_event.sql`, `V3__audit_log_and_reserva.sql`, `V4__materialized_views.sql`). `DATABASE.md` descreve convencoes.
- **Dominio puro**: pacote `api/domain/policy/` concentra `LoanPolicy`, `PenaltyPolicy`, `BookAvailabilityPolicy`, `RequestApprovalPolicy`, `ReservationPolicy` — classes sem dependencia Spring.
- **Observabilidade**: `logback-spring.xml` com JSON encoder prod; `CorrelationIdFilter` popula MDC `X-Correlation-ID`. Actuator expoe health/info/metrics; `BusinessMetricsService` publica metricas de dominio via Micrometer/Prometheus.
- **Novos modulos**: `ReservaService` (fila FIFO com estados), `DashboardService` (metricas agregadas com views materializadas), `RecomendacaoService` (top livros por genero/aluno), `AuditAspect` + `@Auditable` para trilha de auditoria admin.
- **OpenAPI**: `OpenApiConfig` aprimorado com tags, servers, security schemes e examples — pronto para codegen (`orval` no web, `openapi-generator-cli` no app).
- **CI**: `.github/workflows/api.yml` executa build+test em pipeline dedicado.
