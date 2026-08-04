-- =============================================================================
--  V11 - app_version (controle de versão do app / force update)
-- -----------------------------------------------------------------------------
--  Uma linha por plataforma. O app, na inicialização, compara sua versão de
--  runtime com `min_supported_*` e bloqueia se estiver abaixo (ou se
--  `force_update` = TRUE). O admin edita via web (WS-08/WS-09).
-- =============================================================================

CREATE TABLE app_version (
    platform               VARCHAR(20)  NOT NULL,
    latest_version         VARCHAR(20)  NOT NULL,
    latest_build           INTEGER      NOT NULL,
    min_supported_version  VARCHAR(20)  NOT NULL,
    min_supported_build    INTEGER      NOT NULL,
    force_update           BOOLEAN      NOT NULL DEFAULT FALSE,
    update_message         TEXT,
    store_url_android      VARCHAR(512),
    store_url_ios          VARCHAR(512),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by             VARCHAR(100),
    CONSTRAINT pk_app_version PRIMARY KEY (platform),
    CONSTRAINT ck_app_version_platform CHECK (platform IN ('ANDROID', 'IOS')),
    CONSTRAINT ck_app_version_builds_nonneg CHECK (latest_build >= 0 AND min_supported_build >= 0)
);

CREATE TRIGGER trg_app_version_touch BEFORE UPDATE ON app_version
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Seed inicial de referência (alinhado ao pubspec atual 1.1.0+2). Sem RLS ainda.
INSERT INTO app_version (
    platform, latest_version, latest_build, min_supported_version, min_supported_build,
    force_update, update_message, store_url_android, store_url_ios, updated_by
)
VALUES
    ('ANDROID', '1.1.0', 2, '1.0.0', 1, FALSE, NULL,
        'https://play.google.com/store/apps/details?id=br.com.lumilivre.lumilivre', NULL, 'system'),
    ('IOS', '1.1.0', 2, '1.0.0', 1, FALSE, NULL,
        NULL, 'https://apps.apple.com/app/lumilivre', 'system')
ON CONFLICT (platform) DO NOTHING;

-- RLS deny-by-default (padrão V1).
ALTER TABLE app_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_version FORCE  ROW LEVEL SECURITY;
