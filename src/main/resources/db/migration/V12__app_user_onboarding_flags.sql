-- =============================================================================
--  V12 - flags de onboarding em app_user (primeira senha + tour)
-- -----------------------------------------------------------------------------
--  Substitui a inferência frágil de "primeira senha" (comparação de string) por
--  uma flag persistida, e registra a conclusão do tour guiado. Ver WS-10 / SEC-03.
-- =============================================================================

ALTER TABLE app_user
    ADD COLUMN must_change_password  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guided_tour_completed BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: leitores existentes ainda usam senha previsível (= matrícula), então
-- forçam troca no próximo login. Staff (admin/bibliotecário) não é afetado.
-- FORCE RLS filtraria o UPDATE para 0 linhas se o usuário da migration não for
-- superuser (padrão das tabelas desde a V1) — desabilita durante o backfill.
ALTER TABLE app_user DISABLE ROW LEVEL SECURITY;
UPDATE app_user SET must_change_password = TRUE WHERE role = 'READER';
ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
