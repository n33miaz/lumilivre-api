-- =============================================================================
--  V7 - library_settings (tipo de biblioteca como configuracao no banco)
-- -----------------------------------------------------------------------------
--  Tabela single-row que guarda o modo da biblioteca (SCHOOL | STANDARD).
--  Fonte de verdade do GET/PUT /api/settings: o backend deriva as features
--  (academicFields, ranking, thesis) a partir de library_type e web/app
--  escondem ou exibem os recursos academicos de acordo.
--  Default SCHOOL preserva o comportamento atual das instalacoes existentes.
-- =============================================================================

CREATE TABLE library_settings (
    id           BOOLEAN     NOT NULL DEFAULT TRUE,
    library_type VARCHAR(20) NOT NULL DEFAULT 'SCHOOL',
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_library_settings PRIMARY KEY (id),
    CONSTRAINT ck_library_settings_singleton CHECK (id = TRUE),
    CONSTRAINT ck_library_settings_type CHECK (library_type IN ('SCHOOL', 'STANDARD'))
);

INSERT INTO library_settings (id, library_type)
VALUES (TRUE, 'SCHOOL')
ON CONFLICT (id) DO NOTHING;

CREATE TRIGGER trg_library_settings_touch BEFORE UPDATE ON library_settings
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- RLS deny-by-default, igual as demais tabelas de negocio (V1).
ALTER TABLE library_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE library_settings FORCE  ROW LEVEL SECURITY;
