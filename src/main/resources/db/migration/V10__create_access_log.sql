-- =============================================================================
--  V10 - access_log (auditoria de acessos) + audit_log.ip_address
-- -----------------------------------------------------------------------------
--  `access_log` registra eventos de acesso (login/logout/falha/negado) com o
--  IP real do cliente, canal (web/app), user-agent e correlation id. Separado
--  do `audit_log` de negócio por ter volume e semântica próprios.
--  Também adiciona `ip_address` ao `audit_log` para saber de onde partiu a ação.
-- =============================================================================

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

CREATE INDEX idx_access_log_occurred_at ON access_log (occurred_at DESC);
CREATE INDEX idx_access_log_actor       ON access_log (actor);
CREATE INDEX idx_access_log_event       ON access_log (event);
CREATE INDEX idx_access_log_channel     ON access_log (channel);

-- IP real da ação de negócio (preenchido pelo AuditAspect via ClientIpResolver).
ALTER TABLE audit_log ADD COLUMN ip_address VARCHAR(64);

-- RLS deny-by-default (padrão V1/V2).
ALTER TABLE access_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE access_log FORCE  ROW LEVEL SECURITY;
