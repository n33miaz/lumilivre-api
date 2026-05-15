# ADR 006 - Contadores e campos derivados

- **Status**: aceito
- **Data**: 2026-04-23
- **Contexto**: modelagem V2 do schema em ingles
- **Relacionado**: MIGRATION_PLAN.md secao 5.3.2

## Contexto

O schema legacy carrega dois contadores persistidos que sao, por definicao, derivados de outras tabelas:

1. `aluno.emprestimos_count INTEGER NOT NULL DEFAULT 0` - incrementado em `EmprestimoService` no ato de emprestimo; decrementado ao excluir.
2. `livro.quantidade INTEGER` - atualizado em `ImportacaoService` e `LivroService` conforme exemplares sao criados/deletados.

Problemas observados:

- **Drift**: multiplos servicos mexem no contador. Basta um caminho (import, delete em lote, rollback parcial) errar para o numero nao refletir a realidade.
- **Transacionalidade**: o incremento nao esta sempre dentro da mesma transacao que cria a linha correspondente; em falha parcial, o contador dessincroniza.
- **Manutencao cognitiva**: qualquer novo feature que crie/delete emprestimo ou exemplar precisa lembrar de tocar no contador.
- **Relatorios inconsistentes**: ranking de alunos usa `emprestimos_count` e o dashboard conta `emprestimo WHERE status = ATIVO`. Divergencias ja apareceram em testes manuais.

A alternativa natural em Postgres: calcular sob demanda a partir das tabelas-fonte ou via view materializada.

## Decisao

### 1. Remover as colunas derivadas do schema V2

- `student` nao tera `loan_count` / `emprestimos_count`.
- `book` nao tera `quantity` / `quantidade`.

### 2. Calcular sob demanda

Onde o codigo hoje le `aluno.emprestimos_count`:

- **ranking de alunos**: query agregada em `loan` filtrando status terminal.
- **card no dashboard**: materializada no `mv_dashboard_stats` (ja existe; manter).
- **detalhe do aluno**: subquery ou coluna calculada no DTO response.

Onde o codigo hoje le `livro.quantidade`:

- **detalhe do livro / listagem**: `(SELECT COUNT(*) FROM book_copy WHERE book_id = b.id)` ou agregado em view `v_book_stock`.
- **exibicao no catalogo mobile**: incluir `totalCopies` e `availableCopies` ja calculados no DTO.

### 3. View materializada somente onde o custo dominar

Criar `mv_book_stock` (ou incluir colunas em uma view existente) apenas se perfilamento mostrar que `COUNT(*)` em `book_copy` por livro vira gargalo. Volume esperado inicial nao justifica; decidir no PR 5/6 com medicao.

### 4. Se mantermos qualquer contador materializado no futuro

Regras obrigatorias (nao aplicavel agora, apenas documentacao de contrato):

- Atualizar via **trigger no banco**, nunca em servico da aplicacao.
- Ter rotina `REFRESH MATERIALIZED VIEW` documentada e agendada.
- Ter job periodico de reconciliacao que compare contador vs fonte.

Qualquer PR que introduza contador materializado precisa linkar a este ADR e justificar.

## Consequencias

### Positivas

- Uma fonte da verdade: `loan` e `book_copy`.
- Elimina toda uma classe de bug ("contador desbateu da tabela").
- Services ficam mais simples; sem responsabilidade de manter contador.
- Imports em massa nao precisam orquestrar ordem "insere exemplares -> incrementa quantidade".

### Negativas

- Leitura em listagem agora roda `COUNT(*)` por linha. Custo e `O(n)` por pagina; aceitavel nos volumes atuais (milhares de livros, dezenas de milhares de exemplares).
- Clientes que esperavam `quantidade` no response precisam passar a ler `totalCopies` do DTO (mesmo valor, nome novo; mapping no controller preserva compatibilidade PT-BR por ADR-002).

### Migracao de codigo

- `AlunoService.cadastrarEmprestimo` / `excluirEmprestimo`: remover linhas que tocam `emprestimosCount`.
- `LivroService` / `ImportacaoService`: remover `livro.setQuantidade(...)`.
- Ranking e relatorios trocam `ORDER BY emprestimosCount` por `ORDER BY (count agregado)`.
- Testes que validavam o contador viram testes que validam a agregacao.

## Alternativas consideradas

- **Manter contadores, adicionar trigger para reconciliar**: rejeitado. Trigger resolve mas nao justifica a complexidade no volume atual; agregacao direta e suficiente.
- **Usar `statement_timestamp()` + cache na aplicacao**: rejeitado. Cache de contadores em backend vira outra fonte de drift.
- **Adicionar tabela `student_metrics` e atualizar via outbox/event**: rejeitado. Overkill para escala atual; abre espaco se for necessario no futuro.

## Referencias

- MIGRATION_PLAN.md secao 5.3.2
- `docs/schema/prompt_vs_repo_diff.md` secao 5
