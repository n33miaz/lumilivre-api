-- =============================================================================
--  V2__create_outbox_and_audit.sql
-- -----------------------------------------------------------------------------
--  Outbox de eventos (SMTP assincrono) e trilha de auditoria.
--  Tabelas append-only de infraestrutura -> PK BIGINT IDENTITY.
-- =============================================================================

CREATE TABLE outbox_event (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY,
    event_type     VARCHAR(30)  NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject        VARCHAR(255) NOT NULL,
    body           TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at   TIMESTAMPTZ,
    next_retry_at  TIMESTAMPTZ,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id),
    CONSTRAINT ck_outbox_event_status     CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DEAD_LETTER')),
    CONSTRAINT ck_outbox_event_retry_nonneg CHECK (retry_count >= 0)
);

CREATE TABLE audit_log (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY,
    actor          VARCHAR(100) NOT NULL,
    actor_role     VARCHAR(50)  NOT NULL,
    target_id      VARCHAR(200),
    action         VARCHAR(100) NOT NULL,
    result         VARCHAR(20)  NOT NULL,
    error_message  TEXT,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT ck_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

-- RLS deny-by-default.
ALTER TABLE outbox_event ENABLE ROW LEVEL SECURITY; ALTER TABLE outbox_event FORCE ROW LEVEL SECURITY;
ALTER TABLE audit_log    ENABLE ROW LEVEL SECURITY; ALTER TABLE audit_log    FORCE ROW LEVEL SECURITY;
