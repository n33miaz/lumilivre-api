-- ============================================================================
--  V2 - tabelas de infraestrutura
-- ----------------------------------------------------------------------------
--  Append-only, PK BIGINT IDENTITY: outbox de e-mail (entrega assincrona na
--  mesma transacao do dominio), auditoria de negocio e trilha de acessos.
--  Indices ficam na V4.
-- ============================================================================

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
    -- Locale (BCP-47) do destinatario, capturado na publicacao para o envio
    -- assincrono renderizar o e-mail no mesmo idioma do assunto/corpo.
    locale         VARCHAR(10),
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
    ip_address     VARCHAR(64),
    occurred_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT ck_audit_log_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

-- ----------------------------------------------------------------------------
-- access_log: eventos de acesso (login, falha, logout, negado) com IP real,
-- canal, user-agent e correlation id. Separado do audit_log de negocio por
-- ter volume e semantica proprios.
-- ----------------------------------------------------------------------------
CREATE TABLE access_log (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY,
    actor          VARCHAR(100),
    actor_role     VARCHAR(50),
    event          VARCHAR(40)  NOT NULL,
    channel        VARCHAR(20)  NOT NULL,
    result         VARCHAR(20)  NOT NULL,
    ip_address     VARCHAR(64),
    user_agent     VARCHAR(512),
    correlation_id VARCHAR(64),
    error_message  TEXT,
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_access_log PRIMARY KEY (id),
    CONSTRAINT ck_access_log_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

-- ----------------------------------------------------------------------------
-- RLS deny-by-default
-- ----------------------------------------------------------------------------
ALTER TABLE outbox_event ENABLE ROW LEVEL SECURITY; ALTER TABLE outbox_event FORCE ROW LEVEL SECURITY;
ALTER TABLE audit_log    ENABLE ROW LEVEL SECURITY; ALTER TABLE audit_log    FORCE ROW LEVEL SECURITY;
ALTER TABLE access_log   ENABLE ROW LEVEL SECURITY; ALTER TABLE access_log   FORCE ROW LEVEL SECURITY;
