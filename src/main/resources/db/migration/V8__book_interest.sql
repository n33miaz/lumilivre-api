-- ============================================================================
--  V8 - interesse do leitor por livro
-- ----------------------------------------------------------------------------
--  O "curtir" do app morava no SharedPreferences do celular: trocar de
--  aparelho perdia tudo e a biblioteca nunca ficava sabendo o que os alunos
--  querem ler. Esta tabela move o sinal para o servidor, onde ele responde a
--  pergunta que decide compra de acervo: "quantos alunos querem este livro e
--  quantos exemplares temos".
--
--  BOOLEANO, NAO CONTADOR
--    UNIQUE (reader_id, book_id) porque interesse ou existe ou nao existe.
--    Marcar duas vezes nao pode virar dois votos - seria trivial inflar a
--    demanda de um titulo tocando no coracao repetidamente. O unique tambem e
--    o que torna o POST idempotente sem consulta previa.
--
--  ON DELETE - os dois lados sao CASCADE, e por motivos diferentes:
--
--    reader_id -> CASCADE. Aqui e o unico FK para reader que NAO e RESTRICT
--    (loan, reservation e app_user sao), e a diferenca e o que a linha
--    significa. Emprestimo e registro institucional: se alguem apaga o leitor,
--    a biblioteca ainda precisa saber quem esta com o exemplar, entao o banco
--    recusa. Interesse e preferencia de um menor de idade, sem valor
--    institucional nenhum: quando a pessoa e apagada, o rastro do que ela
--    queria ler tem de ir com ela, e um "curtir" jamais pode ser o motivo de
--    uma exclusao falhar.
--
--    book_id -> CASCADE, como book_genre (que tambem e relacao, e nao ativo -
--    book_copy e ativo e por isso e RESTRICT). Interesse so e legivel atraves
--    do livro: "18 alunos queriam <registro apagado>" nao informa compra
--    nenhuma. E RESTRICT aqui transformaria "excluir um livro que alguem
--    curtiu" em erro no painel, porque excluirLivroComExemplares apaga o livro
--    de verdade.
--
--  SEM updated_at (e portanto sem trigger touch_updated_at): interesse nao se
--  edita. Cria-se e apaga-se. created_at serve para ordenar a lista do leitor
--  ("os ultimos que eu curti") e nada mais precisa de data.
--
--  INDICES - as duas leituras que existem, e so elas:
--    1) "os interesses deste leitor" -> ja atendida pelo indice do UNIQUE, que
--       tem reader_id como coluna principal. Criar outro seria indice morto
--       pagando escrita.
--    2) "quantos interesses este livro tem" -> idx_book_interest_book_id, que
--       serve o GROUP BY book_id do resumo em index-only scan.
--
--  RLS deny-by-default como toda tabela desde a V1. Nao inserimos dado aqui
--  (o seed de demonstracao e do R__), entao nao ha DISABLE/ENABLE a fazer.
--
--  FORA DE ORDEM: a V9 entrou antes desta (rodadas paralelas, versoes
--  reservadas por tarefa). Em banco novo as duas aplicam na ordem natural; em
--  banco que ja esta na V9, quem permite a V8 entrar depois e
--  spring.flyway.out-of-order=true - ver o comentario no application.properties.
-- ============================================================================

CREATE TABLE book_interest (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    reader_id  UUID        NOT NULL,
    book_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_book_interest PRIMARY KEY (id),
    CONSTRAINT uq_book_interest_reader_book UNIQUE (reader_id, book_id),
    CONSTRAINT fk_book_interest_reader FOREIGN KEY (reader_id) REFERENCES reader (id) ON DELETE CASCADE,
    CONSTRAINT fk_book_interest_book   FOREIGN KEY (book_id)   REFERENCES book (id)   ON DELETE CASCADE
);

CREATE INDEX idx_book_interest_book_id ON book_interest (book_id);

ALTER TABLE book_interest ENABLE ROW LEVEL SECURITY;
ALTER TABLE book_interest FORCE ROW LEVEL SECURITY;
