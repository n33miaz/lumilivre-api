-- V3: audit_log and reserva tables

CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor           VARCHAR(100) NOT NULL,
    actor_role      VARCHAR(50)  NOT NULL,
    target_id       VARCHAR(200),
    action          VARCHAR(100) NOT NULL,
    result          VARCHAR(20)  NOT NULL,
    error_message   TEXT,
    occurred_at     TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_actor ON audit_log (actor);
CREATE INDEX IF NOT EXISTS idx_audit_log_occurred_at ON audit_log (occurred_at DESC);

CREATE TABLE IF NOT EXISTS reserva (
    id              BIGSERIAL PRIMARY KEY,
    aluno_id        VARCHAR(20)  NOT NULL REFERENCES aluno(matricula),
    livro_id        BIGINT       NOT NULL REFERENCES livro(id),
    status          VARCHAR(40)  NOT NULL DEFAULT 'AGUARDANDO',
    posicao_fila    INTEGER      NOT NULL,
    criada_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    expira_em       TIMESTAMP,
    notificado_em   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reserva_livro_status ON reserva (livro_id, status, posicao_fila);
CREATE INDEX IF NOT EXISTS idx_reserva_aluno ON reserva (aluno_id);

-- Add renovacoes column to emprestimo (default 0 for existing rows)
ALTER TABLE emprestimo ADD COLUMN IF NOT EXISTS renovacoes INTEGER NOT NULL DEFAULT 0;
