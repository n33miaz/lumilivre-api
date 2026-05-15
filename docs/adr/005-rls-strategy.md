# ADR 005 - Row Level Security (RLS)

- **Status**: aceito
- **Data**: 2026-04-23
- **Contexto**: setup do novo banco no Supabase
- **Relacionado**: MIGRATION_PLAN.md secao 3.4; ADR-004

## Contexto

O Supabase expoe automaticamente todas as tabelas do schema `public` via **Data API** (PostgREST). Qualquer chave anonima (`anon`) ou de usuario Supabase pode ler e escrever diretamente nas tabelas, salvo proibicao explicita.

O LumiLivre tem tres caminhos de acesso:

1. **Backend Spring Boot**: conecta via JDBC ao Postgres diretamente, autenticado como `postgres.<ref>`. Esse usuario sempre bypassa RLS.
2. **Storage Supabase**: uploads via API com `service_role` key. Nao toca tabelas de negocio.
3. **Chave publishable no frontend** (`LUMILIVRE_SUPABASE_KEY`): hoje nao deveria ser usada para ler/escrever tabelas; so Storage.

Sem RLS ativa, se o frontend usar a publishable key em um request a tabela `loan`, o Data API responde. Isso e uma exposicao incidental que nao reflete a arquitetura intencional.

## Decisao

Habilitar **RLS deny-by-default em todas as tabelas do schema `public`**, independente do backend usar ou nao o Data API.

### Politica base

Para cada tabela do schema `public`:

```sql
ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <table> FORCE ROW LEVEL SECURITY;  -- nao exceder donos
```

Nenhuma policy criada: resultado e bloqueio total para qualquer role diferente de `postgres`/`service_role`.

`postgres` (owner) e `service_role` sempre bypassam; esse e o papel usado pelo backend Spring Boot e pelo SDK do storage.

### Como o backend continua funcionando

- Conexao JDBC usa `postgres.<ref>` -> bypass.
- `SupabaseStorageService` usa `service_role` key -> bypass.
- Nada mais.

### Quando criar policies explicitas

Soh criar RLS policies se/quando decidirmos expor **alguma** tabela via Data API diretamente ao frontend. Exemplos possiveis no futuro:

- `book` para catalogo publico somente leitura (`SELECT` livre).
- `reservation` para listagem "minhas reservas" pelo usuario autenticado via Supabase Auth.

Nenhum desses esta no escopo da V2. Politicas devem nascer so com demanda de produto, com testes de seguranca dedicados.

## Consequencias

### Positivas

- Defesa em profundidade. Se alguem pegar a publishable key, nao consegue ler/escrever em `loan`, `student`, etc.
- Evita exposicao acidental conforme novas tabelas forem criadas.
- Contratos ficam explicitos: `service_role` e o unico caminho de escrita.

### Negativas

- Desenvolvedores que queiram conectar via SQL editor do Supabase com um role nao-`postgres` precisam estar cientes. Uso normal (via painel como owner) funciona.
- Testes que usem o Data API precisam mockar ou usar service_role.

### Operacionais

- Incluir no baseline V2 (PR 3) um bloco que:
  1. `ALTER TABLE ... ENABLE ROW LEVEL SECURITY; FORCE ...;` para cada tabela.
  2. Sem CREATE POLICY.
- Smoke test manual pos-migration: com a publishable key, tentar `GET /rest/v1/student?select=*` -> deve retornar vazio ou `[]`.

## Buckets de storage

Ortogonal ao RLS de tabelas. Decisoes sobre buckets (ADR separado se crescer, mas ja definido no plano):

- `covers` - publico (capas).
- `theses` - publico se os PDFs forem liberados; privado se conteudo for restrito. Decisao aqui: **publico**, pois sao trabalhos de conclusao publicados.
- `avatars` - privado, acesso via signed URL gerada pelo backend.

Backend usa `service_role` para uploads; frontend busca imagem via URL publica (covers/theses) ou signed URL emitida pela API (avatars).

## Alternativas consideradas

- **Nao habilitar RLS, confiar em nao usar Data API**: rejeitado. Superficie silenciosa se um dia alguem chamar a Data API com a chave publishable.
- **Policies especificas por tabela ja na V2**: rejeitado. Sem consumidor Data API definido, projetar policies e especulativo. Preferimos fechar tudo e abrir sob demanda.
- **Desabilitar o Data API do Supabase**: opcao valida, mas fora do controle via SQL; teria que ser configuracao no painel. RLS deny-by-default atinge o mesmo objetivo via migracao versionada.

## Referencias

- MIGRATION_PLAN.md secao 3.4
- https://supabase.com/docs/guides/database/secure-data
- https://supabase.com/docs/guides/database/postgres/row-level-security
