# Runbook - Ambiente local

> Como subir a LumiLivre API na maquina do dev depois do Supabase estar provisionado (ver `supabase_setup.md`).

## 1. Pre-requisitos

- Java 17 (Temurin recomendado).
- Git Bash no Windows **ou** WSL **ou** PowerShell.
- Docker (opcional, apenas se quiser rodar containerizado).
- Maven Wrapper ja incluso no repo - nao precisa instalar Maven global.

## 2. Clonar e configurar

```bash
git clone git@github.com:n33miaz/lumilivre-api.git
cd lumilivre-api
cp .env.example .env
```

Abrir `.env` e preencher:

- `LUMILIVRE_DB_URL`, `LUMILIVRE_DB_USER`, `LUMILIVRE_DB_PASSWORD` (do Supabase, Session pooler 5432).
- `LUMILIVRE_SUPABASE_URL`, `LUMILIVRE_SUPABASE_KEY`, `LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY`.
- `LUMILIVRE_JWT_SECRET` (gere >= 64 chars: `openssl rand -base64 48`).
- `LUMILIVRE_MAIL_*` (Gmail + App Password ou conta SMTP dedicada).

## 3. Carregar variaveis no shell

### Git Bash / WSL / Linux / macOS

```bash
set -a
source .env
set +a
```

Verificar:

```bash
echo "$LUMILIVRE_DB_URL"
```

### PowerShell

O `.env` no formato `KEY=VALUE` nao e sourcavel direto. Duas opcoes:

**Opcao A** - setar inline a cada sessao:

```powershell
$env:LUMILIVRE_DB_URL = "jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:LUMILIVRE_DB_USER = "postgres.<project-ref>"
$env:LUMILIVRE_DB_PASSWORD = "<senha>"
$env:LUMILIVRE_JWT_SECRET = "<64+ chars>"
$env:LUMILIVRE_SUPABASE_URL = "https://<project-ref>.supabase.co"
$env:LUMILIVRE_SUPABASE_KEY = "<publishable-key>"
$env:LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY = "<service-role-jwt>"
$env:LUMILIVRE_MAIL_USERNAME = "<smtp-user>"
$env:LUMILIVRE_MAIL_PASSWORD = "<smtp-pass>"
$env:LUMILIVRE_CORS_ORIGINS = "http://localhost:5173,http://localhost:8080"
$env:LUMILIVRE_CACHE_TYPE = "simple"
```

**Opcao B** - script helper `.\scripts\load-env.ps1` (criar se preferir):

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
    $name = $Matches[1].Trim()
    $value = $Matches[2].Trim().Trim("'").Trim('"')
    Set-Item -Path "Env:$name" -Value $value
  }
}
```

Usar: `. .\scripts\load-env.ps1` (dot-source para propagar no shell atual).

## 4. Flyway durante a migracao V2

**Flyway esta desabilitado** em `application.properties` (`spring.flyway.enabled=false`). Motivo: o novo baseline em ingles nasce no PR 3; se habilitar agora, as migrations legadas em `db/migration/legacy/` sao aplicadas no banco novo e voce acaba com o schema PT-BR antigo.

Ordem:
1. PR 1 (concluido): discovery e freeze.
2. PR 2 (em andamento): setup de ambiente.
3. PR 3: novo baseline EN - aqui `spring.flyway.enabled=true` volta.

Ate la, o banco continua vazio. O Spring Boot **nao sobe** com `ddl-auto=validate` em banco vazio; para testar conectividade antes do PR 3, usar `psql` ou a query `SELECT 1` pelo Hikari (validation test).

## 5. Subir a aplicacao

Git Bash:

```bash
./mvnw spring-boot:run
```

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Se alguma variavel obrigatoria estiver vazia, o Spring falha com `IllegalArgumentException: Could not resolve placeholder 'LUMILIVRE_...'` antes de subir - isso e esperado e sinaliza a variavel faltando.

## 6. Rodar testes

```bash
./mvnw test
```

Testes atuais ainda usam H2 em alguns pontos. Apos o PR 4, testes de integracao passam a usar Testcontainers PostgreSQL - requer Docker local rodando.

## 7. Executar em container

```bash
docker build -t lumilivre-api .
docker run --rm --env-file .env -p 8080:8080 lumilivre-api
```

No container, `.env` e repassado como `--env-file`. A mesma regra de `KEY=VALUE` sem `export` se aplica - ajustar `.env` se necessario.

## 8. Endpoints uteis depois de subir

- `http://localhost:8080/swagger-ui.html` - documentacao interativa.
- `http://localhost:8080/actuator/health` - status dos componentes.
- `http://localhost:8080/actuator/prometheus` - metricas.

## 9. Troubleshooting rapido

| Sintoma | Causa provavel | Acao |
|---|---|---|
| `Could not resolve placeholder 'LUMILIVRE_*'` | variavel de ambiente nao carregada | rodar `set -a; source .env; set +a` novamente ou setar manualmente |
| `Unknown driver` ou `SQLException: No suitable driver` | URL sem prefixo `jdbc:` | `LUMILIVRE_DB_URL` deve comecar com `jdbc:postgresql://` |
| `FATAL: Tenant or user not found` | user sem sufixo `.project-ref` | `LUMILIVRE_DB_USER=postgres.<project-ref>` |
| `relation "X" does not exist` no startup com `ddl-auto=validate` | Flyway desabilitado em banco vazio | esperar PR 3 (baseline novo) |
| `Spring Data Redis` WARN | cache type simple + jars de Redis | ignorar; so incomoda se `LUMILIVRE_CACHE_TYPE=redis` estiver mal configurado |
| upload Storage `401 Unauthorized` | backend lendo `supabase.key` (anon) em vez de `supabase.service-role.key` | conferir `SupabaseStorageService` apos PR 5 |

## 10. Limpar ambiente local

```bash
./mvnw clean
rm -rf ~/.m2/repository/br/com/lumilivre   # opcional, se quiser forcar resolucao de deps
unset $(grep -E '^[A-Z_]+=' .env | cut -d= -f1)   # limpar variaveis da sessao
```
