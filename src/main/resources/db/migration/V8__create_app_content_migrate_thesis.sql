-- =============================================================================
--  V8 - app_content (generaliza thesis em uma superficie de conteudos)
-- -----------------------------------------------------------------------------
--  Substitui a entidade `thesis` (TCC) por `app_content`, uma tabela unica com
--  discriminador `content_type` (ANNOUNCEMENT / ATTACHMENT / WORK). Alem dos
--  metadados de trabalho academico (ex-thesis), acrescenta os quatro controles
--  de visibilidade que o app do leitor respeita:
--    - is_published        : liga/desliga a visibilidade
--    - is_pinned + display_order : destaque e ordenacao
--    - audience_scope (+ FKs)    : segmentacao de publico (ALL/COURSE/MODULE/SHIFT)
--    - publish_start_at/end_at   : janela de publicacao
--
--  Os TCCs existentes sao migrados como content_type = 'WORK' e a tabela
--  `thesis` e removida. Padrao de RLS/trigger igual ao baseline V1.
-- =============================================================================

CREATE TABLE app_content (
    id                   UUID         NOT NULL DEFAULT gen_random_uuid(),
    content_type         VARCHAR(20)  NOT NULL,
    title                VARCHAR(255) NOT NULL,
    body                 TEXT,
    authors              VARCHAR(500),
    advisors             VARCHAR(500),
    completion_year      INTEGER,
    completion_semester  VARCHAR(10),
    cover_url            VARCHAR(1024),
    file_url             VARCHAR(1024),
    external_url         VARCHAR(1024),
    -- Visibilidade
    is_published         BOOLEAN      NOT NULL DEFAULT TRUE,
    is_pinned            BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order        INTEGER      NOT NULL DEFAULT 0,
    audience_scope       VARCHAR(20)  NOT NULL DEFAULT 'ALL',
    course_id            INTEGER,
    academic_module_id   INTEGER,
    study_shift_id       INTEGER,
    publish_start_at     TIMESTAMPTZ,
    publish_end_at       TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT pk_app_content PRIMARY KEY (id),
    CONSTRAINT fk_app_content_course          FOREIGN KEY (course_id)          REFERENCES course (id)          ON DELETE RESTRICT,
    CONSTRAINT fk_app_content_academic_module FOREIGN KEY (academic_module_id) REFERENCES academic_module (id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_content_study_shift     FOREIGN KEY (study_shift_id)     REFERENCES study_shift (id)     ON DELETE RESTRICT,
    CONSTRAINT ck_app_content_type          CHECK (content_type IN ('ANNOUNCEMENT', 'ATTACHMENT', 'WORK')),
    CONSTRAINT ck_app_content_audience      CHECK (audience_scope IN ('ALL', 'COURSE', 'MODULE', 'SHIFT')),
    CONSTRAINT ck_app_content_year_range    CHECK (completion_year IS NULL OR completion_year BETWEEN 1900 AND 2100),
    CONSTRAINT ck_app_content_window        CHECK (publish_end_at IS NULL OR publish_start_at IS NULL OR publish_end_at >= publish_start_at)
);

-- Feed do leitor: filtra por publicacao + publico.
CREATE INDEX idx_app_content_published_scope ON app_content (is_published, audience_scope);
-- Janela de publicacao (agendado/expirado).
CREATE INDEX idx_app_content_publish_window  ON app_content (publish_start_at, publish_end_at);
-- Ordenacao do mural (destaque primeiro, depois ordem manual).
CREATE INDEX idx_app_content_pinned_order    ON app_content (is_pinned DESC, display_order ASC);
-- FKs de segmentacao.
CREATE INDEX idx_app_content_course_id          ON app_content (course_id);
CREATE INDEX idx_app_content_academic_module_id ON app_content (academic_module_id);
CREATE INDEX idx_app_content_study_shift_id     ON app_content (study_shift_id);

CREATE TRIGGER trg_app_content_touch BEFORE UPDATE ON app_content
    FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ----------------------------------------------------------------------------
-- Migracao de dados: thesis -> app_content (todos viram WORK).
-- `thesis` nasceu com FORCE RLS em V1; desabilita para permitir o SELECT
-- caso o usuario de migracao nao seja superuser. `app_content` ainda nao tem
-- RLS habilitada neste ponto, entao o INSERT flui livremente.
-- ----------------------------------------------------------------------------
ALTER TABLE thesis DISABLE ROW LEVEL SECURITY;

INSERT INTO app_content (
    id, content_type, title, authors, advisors, completion_year, completion_semester,
    cover_url, file_url, external_url, is_published, audience_scope, created_at, updated_at
)
SELECT
    id, 'WORK', title, authors, advisors, completion_year, completion_semester,
    cover_url, pdf_url, external_url, is_active, 'ALL', created_at, updated_at
FROM thesis
WHERE deleted_at IS NULL;

-- Sem CASCADE: `thesis` nao tem dependentes externos (nenhuma FK/view aponta
-- para ela); o indice proprio idx_thesis_course_id cai junto sem NOTICE.
DROP TABLE thesis;

-- RLS deny-by-default, igual as demais tabelas de negocio (V1).
ALTER TABLE app_content ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_content FORCE  ROW LEVEL SECURITY;
