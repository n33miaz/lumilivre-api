# Runbook - Setup de projeto Supabase para LumiLivre

> Passo a passo para provisionar um ambiente Supabase novo e deixar o backend Spring Boot pronto para rodar. Cobre `dev` e `prod` em paralelo.
> Referencias: MIGRATION_PLAN.md secao 4; ADR-004 (conexao), ADR-005 (RLS).

## 1. Pre-requisitos

- Conta Supabase com billing habilitado (plano Free ou superior).
- Acesso ao repositorio `lumilivre-api` com permissao de escrita no `.env`.
- Password manager / cofre para guardar segredos (nao use `.env` como cofre).

## 2. Criar organizacao e projetos

1. Criar organizacao `lumilivre` (ou reaproveitar existente).
2. Criar dois projetos separados:
   - `lumilivre-dev`
   - `lumilivre-prod`
3. Regiao recomendada: `South America (Sao Paulo)` ou a mais proxima do publico.
4. Guardar a senha do banco gerada na criacao - ela so aparece uma vez.

## 3. Habilitar extensoes obrigatorias

Em cada projeto, `Database -> Extensions` ou via SQL editor:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
-- opcional conforme perfilamento:
-- CREATE EXTENSION IF NOT EXISTS btree_gin;
```

No PR 3 (novo baseline), essas chamadas entram como primeira migracao para garantir idempotencia.

## 4. Coletar credenciais

No painel do projeto, `Project Settings -> API`:

- `Project URL` -> `LUMILIVRE_SUPABASE_URL`
- `anon` ou `publishable` key -> `LUMILIVRE_SUPABASE_KEY`
- `service_role` key (revelar) -> `LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY`

Em `Project Settings -> Database -> Connection string`:

- Escolher **Session pooler** (porta 5432).
- Copiar o host (`aws-*.pooler.supabase.com`).
- A string final do `.env`:

```
LUMILIVRE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
LUMILIVRE_DB_USER=postgres.<project-ref>
LUMILIVRE_DB_PASSWORD=<senha-do-banco>
```

**Nao use a porta 6543 (Transaction pooler)**. Motivo em ADR-004.

## 5. Criar buckets de Storage

No painel, `Storage -> New bucket`:

| Bucket | Visibilidade | Uso |
|---|---|---|
| `covers` | publico | capas de livros |
| `theses` | publico | PDFs de TCC |
| `avatars` | **privado** | fotos de aluno (acesso via signed URL emitida pelo backend) |

Em `avatars`, criar politica de acesso apenas para o `service_role` (default quando o bucket e privado).

## 6. Politica RLS nas tabelas

A V2 (PR 3) vai habilitar RLS deny-by-default em todo `public`. **Nao criar policies manuais** fora da migracao - todas nascem versionadas no baseline.

Apos o PR 3 rodar, validar com a publishable key:

```bash
curl -H "apikey: $LUMILIVRE_SUPABASE_KEY" \
     -H "Authorization: Bearer $LUMILIVRE_SUPABASE_KEY" \
     "$LUMILIVRE_SUPABASE_URL/rest/v1/student?select=*"
```

Resposta esperada: `[]` ou erro de permissao. Nunca retornar linhas com essa chave.

## 7. Popular segredos no repositorio local

1. `cp .env.example .env`
2. Substituir todos os placeholders pelos valores do painel.
3. Carregar no shell conforme `docs/runbooks/local_environment.md`.
4. Teste rapido de conexao antes de rodar a aplicacao:

```bash
psql "postgresql://$LUMILIVRE_DB_USER:$LUMILIVRE_DB_PASSWORD@$(echo $LUMILIVRE_DB_URL | sed 's/jdbc://' | sed 's/\?.*//' | sed 's|^postgresql://||')/postgres?sslmode=require" -c 'SELECT 1;'
```

Ou, se preferir evitar montar a URL completa no shell, usar a connection string pronta do painel.

## 8. Configurar ambiente de CI

Em GitHub Actions (ou equivalente):

- Secrets obrigatorios:
  - `LUMILIVRE_DB_URL`
  - `LUMILIVRE_DB_USER`
  - `LUMILIVRE_DB_PASSWORD`
  - `LUMILIVRE_SUPABASE_URL`
  - `LUMILIVRE_SUPABASE_KEY`
  - `LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY`
  - `LUMILIVRE_JWT_SECRET` (um secret random por ambiente; nao reutilizar entre dev/prod).
  - `LUMILIVRE_MAIL_*` (conta SMTP dedicada para CI, se houver).

O workflow `.github/workflows/api.yml` ja consome essas variaveis; so cadastrar valores.

## 9. Rotacao de credenciais

- Senha do banco: no painel `Settings -> Database -> Reset database password`. Atualiza so `LUMILIVRE_DB_PASSWORD` nos ambientes.
- `service_role` key: `Project Settings -> API -> Reset service role key`. Atualiza `LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY` em backend e CI. Chaves antigas continuam validas ate a rotacao propagar (~segundos).
- `publishable/anon` key: idem, mas propaga para Web/App (considerar impacto em apps ja publicados).

## 10. Checklist pos-setup

- [ ] Projetos `dev` e `prod` criados.
- [ ] Extensoes habilitadas.
- [ ] Buckets `covers`, `theses`, `avatars` criados com visibilidade correta.
- [ ] Credenciais salvas no gerenciador e no `.env` local.
- [ ] `psql` executa `SELECT 1` com sucesso.
- [ ] CI secrets cadastrados.
- [ ] Flyway continua `enabled=false` no `application.properties` ate o PR 3.

## 11. Troubleshooting comum

- **"FATAL: Tenant or user not found"**: o `LUMILIVRE_DB_USER` deve ser `postgres.<project-ref>`, nao so `postgres`. Projeto e multi-tenant no pooler.
- **Handshake SSL falha**: confirmar que o URL tem `?sslmode=require` e que `spring.datasource.hikari.data-source-properties.sslmode=require` esta no `application.properties`.
- **Prepared statement does not exist**: se aparecer, voce provavelmente esta no pooler transacional 6543. Voltar para 5432 (Session pooler).
- **Upload 401 no Storage**: backend esta usando a `anon` key por engano. Confirmar que `SupabaseStorageService` consome `supabase.service-role.key`.
