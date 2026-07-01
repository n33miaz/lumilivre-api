# Migração de sistemas legados para LumiLivre

> Versão: 2026-05-22.
> Audiência: DBAs, bibliotecários e analistas de TI que precisam migrar
> dados de um software de gestão de biblioteca preexistente para o LumiLivre.

Este guia cobre **três fluxos práticos** de migração:

1. Importação via planilhas XLSX (caminho recomendado para volumes < 10k linhas).
2. Importação direta no banco via SQL `INSERT ... SELECT`.
3. Mapeamento de campos típicos de Pergamum, Biblivre e OPAC.

> Pré-requisito: schema LumiLivre criado a partir de `ddl_standalone.sql`
> ou aplicado via Flyway (`V1..V5`).

---

## 1. Via planilhas XLSX (fluxo padrão)

A LumiLivre API expõe `POST /api/import?tipo={leitor|livro|exemplar}`
(multipart `.xlsx`) com validação, deduplicação e processamento em lotes
de 50. Esse caminho:

- preserva todos os invariantes (`BookService`, `ReaderService`),
- cria automaticamente o `app_user` com `role=READER` para cada leitor,
- registra erros linha-a-linha em um relatório retornado pela API,
- é idempotente — leitores/livros/exemplares já existentes são reportados
  como duplicidade, sem sobrescrever.

### 1.1 Planilha de **leitores**

Cabeçalhos esperados (case-insensitive, espaços → `_`):

| Coluna obrigatória  | Exemplo            | Notas                                     |
|---------------------|--------------------|-------------------------------------------|
| matricula           | `2024001`          | Único; vira `registration_number`.        |
| nome_completo       | `Ana Beatriz Lima` |                                           |
| curso_id            | `1`                | ID válido em `course`.                    |
| turno_id            | `1`                | ID válido em `study_shift`.               |
| modulo_id           | `2`                | ID válido em `academic_module`.           |

| Coluna opcional      | Exemplo                  | Notas                                          |
|----------------------|--------------------------|------------------------------------------------|
| cpf                  | `52998224725`            | Apenas dígitos; senão é normalizado.           |
| celular              | `11990010001`            |                                                |
| email                | `ana.lima@example.com`   | Cria `app_user` com senha = matrícula.         |
| data_nascimento      | `12/04/2007`             | Formatos aceitos: ISO ou DD/MM/YYYY.           |
| cep, logradouro, bairro, localidade, uf, numero_casa, complemento | — | Endereço.                                  |

### 1.2 Planilha de **livros**

| Coluna obrigatória   | Exemplo                  | Notas                                         |
|----------------------|--------------------------|-----------------------------------------------|
| nome                 | `Dom Casmurro`           | Vira `title`.                                 |
| autor                | `Machado de Assis`       |                                               |
| editora              | `Editora Ficticia Escola`|                                               |
| cdd_codigo           | `869`                    | Deve existir em `dewey_classification`.       |

| Coluna opcional        | Exemplo               | Notas                                         |
|------------------------|-----------------------|-----------------------------------------------|
| isbn                   | `9788535902778`       | 10 ou 13 dígitos.                             |
| data_lancamento        | `01/01/1899`          | Não pode ser futura.                          |
| numero_paginas         | `256`                 | > 0.                                          |
| edicao, volume, sinopse| —                     |                                               |
| imagem                 | URL                   | Vira `cover_url`.                             |
| classificacao_etaria   | `GENERAL`/`TEEN`/`ADULT` | Default `GENERAL`.                            |
| tipo_capa              | `PAPERBACK`/`HARDCOVER`/`SOFTCOVER` | Default `PAPERBACK`.                |

### 1.3 Planilha de **exemplares**

| Coluna obrigatória   | Exemplo                   | Notas                                         |
|----------------------|---------------------------|-----------------------------------------------|
| tombo                | `LUM-0001`                | Único; vira `copy_code`.                      |
| livro_id             | UUID                      | Pegue da listagem de livros recém-importada.  |
| localizacao_fisica   | `A1-01`                   | Etiqueta de prateleira.                       |

| Coluna opcional      | Exemplo                                | Notas                                            |
|----------------------|----------------------------------------|--------------------------------------------------|
| status_livro         | `AVAILABLE`/`BORROWED`/`MAINTENANCE`/`UNAVAILABLE` | Default `AVAILABLE`.            |

### 1.4 Fluxo recomendado

1. Importe **cursos / módulos / turnos / CDD** manualmente (ou rode
   `V5__seed_reference_data.sql` se o catálogo PT-BR padrão for suficiente).
2. Importe **leitores**.
3. Importe **livros**.
4. Liste os livros importados (`GET /api/books`) para descobrir os UUIDs
   gerados; preencha a coluna `livro_id` da planilha de **exemplares**.
5. Importe **exemplares**.
6. (Opcional) Importe históricos via SQL — veja §2.

---

## 2. Importação direta via SQL

Útil quando o legado tem milhões de linhas e o caminho XLSX seria lento.

> Recomenda-se trabalhar com **schema de staging** separado para isolar
> o legado, e migrar para o `public` após sanitização.

```sql
-- Schema temporário para receber o dump bruto do legado.
CREATE SCHEMA legacy;

-- Exemplo: tabela de leitores legados sem mudar nomes.
CREATE TABLE legacy.leitores (
    matricula  VARCHAR(20),
    nome       VARCHAR(255),
    cpf        VARCHAR(14),
    email      VARCHAR(255),
    curso_str  VARCHAR(120),
    turno_str  VARCHAR(50),
    modulo_str VARCHAR(50),
    -- ...
);

-- Carga (pg_dump | psql | COPY FROM CSV).
\copy legacy.leitores FROM 'leitores_legacy.csv' WITH (FORMAT csv, HEADER true);

-- Sanitização básica: dígitos do CPF, validação de email.
UPDATE legacy.leitores SET cpf = REGEXP_REPLACE(cpf, '\D', '', 'g');

-- Inserção em LumiLivre resolvendo FK por nome.
INSERT INTO reader (
    registration_number, full_name, cpf, email,
    course_id, academic_module_id, study_shift_id
)
SELECT
    l.matricula,
    l.nome,
    NULLIF(l.cpf, ''),
    NULLIF(l.email, ''),
    c.id, m.id, s.id
FROM legacy.leitores l
JOIN course           c ON c.name = l.curso_str
JOIN academic_module  m ON m.name = l.modulo_str
JOIN study_shift      s ON s.name = l.turno_str
ON CONFLICT (registration_number) DO NOTHING;
```

> Atenção: o caminho SQL **não cria `app_user`** para READER. Se quiser
> permitir login dos leitores importados, gere os usuários separadamente
> com BCrypt — a senha inicial é a matrícula. Exemplo em Python:
> `from bcrypt import hashpw, gensalt; hashpw(b"2024001", gensalt(rounds=10))`.

---

## 3. Mapeamento por sistema legado

### 3.1 Pergamum (Universidade Federal do Paraná / Fundecto)

Pergamum usa um schema relacional próprio (`acervo`, `acervo_emprestimo`,
`acervo_assunto`). Mapeamento alto nível:

| Pergamum                     | LumiLivre                              |
|------------------------------|----------------------------------------|
| `acervo` (registro)          | `book`                                 |
| `acervo_titulo`              | `book.title`                           |
| `acervo_isbn`                | `book.isbn`                            |
| `acervo_autor`               | `book.author`                          |
| `acervo_editora`             | `book.publisher`                       |
| `acervo_classificacao`       | `book.dewey_code`                      |
| `acervo_assunto.assunto_str` | `genre.name` (após de-duplicação)      |
| `exemplar`                   | `book_copy`                            |
| `exemplar.tombo`             | `book_copy.copy_code`                  |
| `exemplar.localizacao`       | `book_copy.shelf_location`             |
| `exemplar.situacao`          | `book_copy.status` (mapear códigos)    |
| `usuario` (leitor)            | `reader`                              |
| `usuario.matricula`          | `reader.registration_number`          |
| `acervo_emprestimo`          | `loan` (somente concluídos no legado)  |

Estratégia recomendada:

1. Exportar Pergamum em CSV via console DBA.
2. Importar para schema `legacy` no Postgres LumiLivre.
3. Resolver gêneros mapeando `acervo_assunto` para `genre` (criar gêneros
   ausentes manualmente). Limitar a 3 por livro.
4. Importar `book`, `book_copy`, `reader` na ordem.
5. Importar `loan` somente se houver interesse de histórico — caso
   contrário, importe só os ativos (`status IN ('Emprestado', 'Atrasado')`).

### 3.2 Biblivre 5 (Itautec / Biblivre)

Biblivre usa formato MARC para registros. Para migrar:

1. Exportar acervo em `.iso` (ISO-2709) via menu `Acervo → Exportar`.
2. Converter para CSV usando MarcEdit ou pymarc.
3. Mapear MARC 245 (`title`), 100 (`author`), 020 (`isbn`), 260 (`publisher`),
   082 (`dewey_code`) para colunas LumiLivre.
4. Seguir do §2 em diante.

### 3.3 OPAC genérico (Sophia / Aurora / Athena)

Sistemas OPAC tipicamente exportam CSV. Mapeamento mínimo:

| OPAC genérico    | LumiLivre                |
|------------------|--------------------------|
| `titulo`         | `book.title`             |
| `autor`          | `book.author`            |
| `isbn`           | `book.isbn`              |
| `editora`        | `book.publisher`         |
| `ano`            | `book.publication_date` (1º de janeiro) |
| `cdd`            | `book.dewey_code`        |
| `assunto`        | `genre.name`             |
| `numero_chamada` | `book_copy.shelf_location` |
| `tombo`          | `book_copy.copy_code`    |

---

## 4. Validações pós-migração

```sql
-- 1. Integridade referencial — todos os exemplares apontam para livros válidos.
SELECT bc.copy_code
FROM book_copy bc
LEFT JOIN book b ON b.id = bc.book_id
WHERE b.id IS NULL;

-- 2. Status agregado.
SELECT status, COUNT(*) FROM book_copy GROUP BY status ORDER BY status;

-- 3. CDDs presentes em book mas ausentes em dewey_classification.
SELECT DISTINCT b.dewey_code
FROM book b
LEFT JOIN dewey_classification d ON d.code = b.dewey_code
WHERE b.dewey_code IS NOT NULL AND d.code IS NULL;

-- 4. Leitores sem app_user (login indisponível).
SELECT s.registration_number, s.full_name
FROM reader s
LEFT JOIN app_user a ON a.reader_id = s.id
WHERE a.id IS NULL;

-- 5. Empréstimos com inconsistências de estado.
SELECT l.id, l.status, bc.status
FROM loan l
JOIN book_copy bc ON bc.id = l.book_copy_id
WHERE (l.status IN ('ACTIVE','OVERDUE') AND bc.status <> 'BORROWED')
   OR (l.status = 'COMPLETED' AND bc.status = 'BORROWED'
        AND NOT EXISTS (SELECT 1 FROM loan x WHERE x.book_copy_id = bc.id AND x.status IN ('ACTIVE','OVERDUE')));
```

---

## 5. Pós-migração: refresh do dashboard

Após qualquer carga massiva, refrescar as views materializadas:

```sql
REFRESH MATERIALIZED VIEW mv_dashboard_stats;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_top_books;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_loans_by_month;
```

O backend faz isso automaticamente a cada 15 minutos via `DashboardService`,
mas o refresh manual evita esperar para validar contagens no painel.

---

## 6. Rollback de uma migração problemática

Se a migração corromper dados de produção:

1. Sempre faça `pg_dump` antes da migração:
   `pg_dump -Fc -d lumilivre > backup_pre_migration.dump`.
2. Para reverter:
   `pg_restore -d lumilivre --clean --if-exists backup_pre_migration.dump`.
3. Em deploy Supabase, use o **Point-in-Time Restore** do painel para
   voltar a um instante anterior (planos Pro+).

---

## 7. Referências

- DDL completo: [`ddl_standalone.sql`](./ddl_standalone.sql)
- Dicionário: [`data_dictionary.md`](./data_dictionary.md)
- Portabilidade entre bancos: [`portability_notes.md`](./portability_notes.md)
- Endpoints REST de importação: `lumilivre-api/docs/runbooks/supabase_setup.md`
  e `OpenAPI /docs` (Swagger UI).
