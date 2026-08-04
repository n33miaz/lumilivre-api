-- =============================================================================
--  V9 - permissao global de troca de foto pelo app (reader_can_edit_avatar)
-- -----------------------------------------------------------------------------
--  Toggle unico em library_settings (single-row) que controla se os leitores
--  podem trocar a propria foto pelo app. Default TRUE preserva o comportamento
--  atual. A UI de administracao expoe o toggle; a API reforca a regra
--  no upload self-service (defesa em profundidade).
-- =============================================================================

ALTER TABLE library_settings
    ADD COLUMN reader_can_edit_avatar BOOLEAN NOT NULL DEFAULT TRUE;
