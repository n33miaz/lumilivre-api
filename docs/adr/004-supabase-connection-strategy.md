# ADR 004 - Estrategia de conexao com Supabase

- **Status**: aceito
- **Data**: 2026-04-23
- **Contexto**: setup do novo banco no Supabase (fase 1 do MIGRATION_PLAN)
- **Relacionado**: MIGRATION_PLAN.md secao 4.2, referencia operacional 12

## Contexto

O Supabase oferece tres formas de conexao ao Postgres gerenciado:

1. **Direct connection** (`db.<ref>.supabase.co:5432`): TCP direto. Historicamente exige IPv6; hoje ha opcoes de `ipv4 add-on` pagas.
2. **Session pooler** (`aws-*.pooler.supabase.com:5432`): via Supavisor em modo `session`. Comporta prepared statements e conexoes persistentes.
3. **Transaction pooler** (`aws-*.pooler.supabase.com:6543`): via Supavisor em modo `transaction`. Otimo para workloads serverless (curtas, sem prepared statements cacheados no servidor).

O backend LumiLivre e um servidor persistente Spring Boot + HikariCP + Hibernate, com:

- pool de 2-10 conexoes vivas (`hikari.minimum-idle=2`, `hikari.maximum-pool-size=10`).
- prepared statements cacheados pelo driver.
- transacoes longas em alguns services (importacao, dashboard refresh).

Usar o transaction pooler (6543) com esse perfil causa:

- perda de prepared statements entre transacoes (Supavisor nao os mantem em modo transaction).
- avisos do driver PG sobre statements invalidados.
- overhead sem beneficio (nao temos bursts serverless).

## Decisao

Padrao de conexao do backend: **Session pooler na porta 5432**.

String atual (`.env`):

```
LUMILIVRE_DB_URL=jdbc:postgresql://aws-1-sa-east-1.pooler.supabase.com:5432/postgres?sslmode=require
LUMILIVRE_DB_USER=postgres.<project-ref>
LUMILIVRE_DB_PASSWORD=<db-password>
```

Regras:

- `sslmode=require` obrigatorio.
- Credenciais fora da URL; usar `LUMILIVRE_DB_USER` e `LUMILIVRE_DB_PASSWORD` em variaveis separadas (ja aplicado no `.env` pos-correcao).
- Mesma string para Flyway na primeira onda; se for necessario split, criar `LUMILIVRE_DB_URL_MIGRATION`.
- Nao usar `6543` nem para runtime nem para Flyway.
- Direct connection so e considerada se o ambiente de runtime suportar IPv6 e houver ganho mensuravel.

## Consequencias

### Positivas

- Prepared statements do driver funcionam sem warnings.
- Compativel com o comportamento esperado do Hibernate em servidor persistente.
- Mesma porta (5432) que uma conexao direta, facilitando transicao futura.

### Negativas

- Session pooler impoe um teto de conexoes simultaneas por projeto Supabase. Se o backend escalar horizontalmente, somar `hikari.maximum-pool-size * N instancias` precisa caber no limite.
- Transacoes longas (> minutos) podem segurar conexao no pooler mais tempo que o ideal.

### Operacionais

- `hikari.max-lifetime=1800000` (30 min) ja esta conservador; manter.
- `hikari.idle-timeout=600000` (10 min) alinhado com o que o Supavisor tolera.
- Monitorar `pg_stat_activity` e alertas do Supabase de "max connections reached" em janelas de importacao.

## Observacoes operacionais

- A senha de banco do Supabase e rotacionavel no painel; a URL de pooler nao muda.
- O backend escreve em `LUMILIVRE_DB_URL` so o host+port+db+sslmode; sem user/pass embedded. Isso permite rotacionar a senha alterando apenas `LUMILIVRE_DB_PASSWORD`.
- Migrations Flyway ficam desabilitadas (`spring.flyway.enabled=false`) ate o PR 3 publicar o novo baseline em ingles.

## Alternativas consideradas

- **Transaction pooler (6543) como padrao**: rejeitado. Incompativel com prepared statements do driver JDBC PG + perfil persistente do backend.
- **Direct connection como padrao**: rejeitado por enquanto. Ambiente de execucao atual nao garante IPv6; se migrar para plataforma com suporte (Fly.io, ECS Fargate + IPv6 egress), reavaliar.
- **Conexoes duplas** (app via 5432, Flyway via 6543): rejeitado. Sem ganho e adiciona superficie de configuracao.

## Referencias

- MIGRATION_PLAN.md secao 4.2
- https://supabase.com/docs/guides/troubleshooting/supavisor-and-connection-terminology-explained-9pr_ZO
- https://supabase.com/docs/reference/postgres/connection-strings
