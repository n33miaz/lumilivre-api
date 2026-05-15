# ADR 002 - Estrategia de contrato HTTP

- **Status**: aceito
- **Data**: 2026-04-23
- **Contexto**: fase zero da migracao V2 (schema + backend em ingles)
- **Relacionado**: MIGRATION_PLAN.md secoes 3.1, 6.2 e 6.5; ADR-001

## Contexto

A migracao V2 renomeia tabelas, colunas, entidades JPA, repositories e services de PT-BR para EN. Os clientes atuais (`lumilivre-web`, `lumilivre-app`) consomem a API com contratos em PT-BR:

- rotas: `/alunos`, `/livros`, `/emprestimos`, `/solicitacoes`, `/reservas`, `/tcc`, `/cursos`, `/turnos`, `/modulos`, `/generos`, `/cdd`, `/relatorios`, `/importacao`, `/dashboard`, `/auth`, `/usuarios`.
- payloads: campos como `matricula`, `nomeCompleto`, `dataEmprestimo`, `exemplarTombo`, `statusEmprestimo`.
- enums persistidos e retornados em PT-BR (`ATIVO`, `PENDENTE`, `AGUARDANDO`, `BIBLIOTECARIO`).

Quebrar esse contrato em uma unica onda geraria:

- coordenacao com dois projetos cliente (web + mobile) no mesmo PR.
- risco alto em fluxos criticos (login, solicitacao, emprestimo, devolucao).
- janela de incompatibilidade binaria no mobile ate nova release publicada.

## Decisao

Na **v1 da API**, o contrato HTTP permanece em PT-BR. A refatoracao para EN e **interna** ao backend.

Camadas:

- **Controller + DTO publico**: nomes, campos, rotas e enums em PT-BR.
- **Service, domain model, entity, repository, SQL nativo**: tudo em EN.
- **Mapper**: DTO PT-BR (externo) <-> modelo EN (interno). Cada endpoint que hoje retorna um `AlunoResponse` continua retornando o mesmo JSON, mas o corpo e montado a partir de um `Student` interno.
- **Enums em request/response**: mantidos como string PT-BR no JSON; o mapper traduz para `LoanStatus`, `Role`, etc no service.

Isso vale para:

- 100% dos endpoints atuais
- respostas de erro (`ErroResponse`)
- estruturas de dashboard, relatorios e importacao

### Exemplo pratico

`POST /alunos` continua recebendo:

```json
{
  "matricula": "12345",
  "nomeCompleto": "Maria da Silva",
  "cursoId": 2,
  "turnoId": 1,
  "moduloId": 3
}
```

Internamente o service recebe um `Student` com `registrationNumber`, `fullName`, `courseId`, `studyShiftId`, `academicModuleId`.

`GET /emprestimos/ativos` continua retornando `statusEmprestimo: "ATIVO"`; o enum interno `LoanStatus.ACTIVE` vira string PT-BR no mapper.

### Quando a API vira v2

Evolucao futura (fora desta migracao):

- expor `/v2/**` com nomes em ingles e enums ingleses.
- manter `/v1/**` (atual) ate todos os clientes migrarem.
- deprecar com header `Sunset`.

Criterios para iniciar v2: stack cliente unificada, ciclo de release curto no mobile, tooling de geracao de cliente a partir de OpenAPI.

## Consequencias

### Positivas

- Nenhuma quebra de cliente na V2 do banco.
- Refatoracao interna fica isolada; PRs menores, testaveis.
- Abre espaco para evoluir modelo interno sem acoplar a versao externa.

### Negativas

- Camada de mapper fica explicita em 100% dos endpoints. Boilerplate cresce.
- Enums precisam de conversao bidirecional; mismatch silencioso e possivel se nao for testado.
- Time precisa aceitar "duas linguagens" no mesmo PR (classe Java em EN, DTO em PT-BR).

### Mitigacoes

- Tests de contrato automatizados em endpoints criticos (`/auth/login`, `/emprestimos`, `/solicitacoes/processar`).
- Mappers centralizados por modulo, nunca inline no controller.
- Helpers para conversao de enum: `PenaltyCode.fromLabel(String)` e `toLabel()`; falha rapida em valor desconhecido.
- OpenAPI documenta apenas o formato PT-BR externo (SpringDoc le o DTO).

## Alternativas consideradas

- **Traduzir contrato na mesma onda**: rejeitado. Quebra dois clientes simultaneamente com risco alto de regressao.
- **Manter entidades em PT-BR internamente tambem**: rejeitado. Perde o objetivo da migracao V2 e continua misturando idiomas.
- **v2 agora, deprecar v1 em paralelo**: rejeitado. Dobra esforco; usuario final nao ganha nada; atrasa entregas.

## Referencias

- MIGRATION_PLAN.md secao 6.2 (regra de desacoplamento) e 6.5 (decisao sobre contrato HTTP)
- SCHEMA_DICTIONARY.md secao 8
