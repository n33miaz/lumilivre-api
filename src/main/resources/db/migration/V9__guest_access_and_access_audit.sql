-- ============================================================================
--  V9 - acesso de convidado + alvo na trilha de acessos
-- ----------------------------------------------------------------------------
--  Duas mudancas pequenas que servem ao mesmo objetivo: saber, depois, quem
--  usou a biblioteca e sob quais regras.
--
--  1) library_settings.guest_access_enabled
--     O app tem um modo convidado (catalogo aberto, sem login). Ele era
--     incondicional: nao havia como uma escola dizer "aqui o acervo so aparece
--     para aluno identificado". A flag nasce TRUE porque desligar por default
--     mudaria o comportamento de quem ja usa o app.
--
--  2) access_log.target_id
--     Ate aqui o access_log respondia "quem entrou". Passa a responder tambem
--     "o que a pessoa consultou" - e sem uma coluna de alvo isso e impossivel:
--     "quais livros os alunos procuram e a biblioteca nao tem" e "quem abriu
--     este comunicado" precisam do identificador do recurso. Empurrar o id
--     dentro de error_message seria abusar de uma coluna de diagnostico.
--     VARCHAR(200) para acompanhar audit_log.target_id.
--
--  RETENCAO (nota deliberada, sem job automatico)
--     Com 50 leitores de demonstracao o volume e irrelevante (~500 linhas/dia).
--     Numa escola de 800 alunos sao ~8 mil linhas/dia, ~3 milhoes/ano, algo
--     como 600 MB com indices. A recomendacao e manter 12 meses "quentes" e
--     arquivar/descartar o resto:
--         DELETE FROM access_log WHERE occurred_at < now() - INTERVAL '12 months';
--     O indice (event, occurred_at DESC) criado abaixo cobre esse DELETE.
--     Nao criamos o job aqui de proposito: apagar trilha de auditoria e decisao
--     de politica da instituicao, nao default de software - um purge silencioso
--     e pior que uma tabela grande.
--
--  RLS: as duas tabelas ja nasceram com ENABLE + FORCE ROW LEVEL SECURITY
--  (library_settings na V3, access_log na V2) e ADD COLUMN nao mexe nisso.
--  library_settings tambem conserva o trigger trg_library_settings_touch.
-- ============================================================================

ALTER TABLE library_settings
    ADD COLUMN guest_access_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE access_log
    ADD COLUMN target_id VARCHAR(200);

-- (event, occurred_at DESC): responde as perguntas de coordenacao ("quantos
-- alunos consultaram o acervo esta semana") e serve ao purge por data. Os
-- indices da V4 sao todos de coluna unica, entao esse recorte terminava em
-- bitmap scan + filtro.
CREATE INDEX idx_access_log_event_occurred_at
    ON access_log (event, occurred_at DESC);

-- (target_id, occurred_at DESC) parcial: so linhas de uso tem alvo, e login /
-- logout / falha (a maioria absoluta) ficam fora do indice.
CREATE INDEX idx_access_log_target_occurred_at
    ON access_log (target_id, occurred_at DESC)
    WHERE target_id IS NOT NULL;
