-- ============================================================================
--  V7 - status de conta e revogacao de token
-- ----------------------------------------------------------------------------
--  Antes desta versao demitir alguem nao tirava o acesso: o login ignorava
--  deleted_at e nao havia como suspender uma conta sem apagar a linha. O JWT
--  valia 24h e nao existia nenhuma forma de revogar um token vazado.
--
--  active  -> desligamento administrativo (afastamento, desligamento).
--  locked  -> bloqueio por seguranca (suspeita de comprometimento).
--  Sao dois motivos diferentes e a UI precisa distinguir, por isso duas colunas
--  e nao um enum de status.
--
--  token_version da revogacao imediata SEM Redis (que e opcional no projeto):
--  o token carrega a versao vigente na emissao e o filtro exige igualdade
--  exata, entao qualquer incremento invalida na hora tudo que ja foi emitido.
--  Trocar senha, resetar senha, desativar/bloquear a conta e o logout
--  incrementam.
--
--  Contador e nao timestamp de proposito: comparar o "iat" do JWT (precisao de
--  segundo) com um instante de corte (precisao de microssegundo) deixa uma
--  janela de ate 1s em que o logout NAO revoga o token emitido no mesmo
--  segundo -- e arredondar o corte para cima so troca o defeito de lado
--  (passa a nascer revogado o token novo). Um inteiro monotonico nao tem
--  granularidade nem relogio: ou a versao bate, ou o token morreu.
-- ============================================================================

ALTER TABLE app_user
    ADD COLUMN active        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN locked        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

-- O contador nunca anda para tras: se andasse, tokens antigos voltariam a valer.
ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_token_version_nonneg CHECK (token_version >= 0);

-- Efeito colateral desejado: token emitido antes deste deploy nao carrega a
-- claim de versao e o filtro recusa token sem claim (falha fechado). As sessoes
-- atuais morrem todas de uma vez -- a rotacao que a auditoria pedia junto com a
-- rotacao da senha do admin de producao.

-- A regra "nao desative o ultimo ADMIN ativo" conta ADMINs ativos em cada
-- mudanca de status; sem indice isso vira seq scan em app_user.
CREATE INDEX idx_app_user_role_active
    ON app_user (role)
    WHERE active AND deleted_at IS NULL;

-- O login agora filtra deleted_at IS NULL; o indice parcial cobre a busca por
-- matricula do leitor, que nao tem unique proprio em app_user.
CREATE INDEX idx_app_user_reader_id_alive
    ON app_user (reader_id)
    WHERE deleted_at IS NULL;

-- RLS: app_user ja nasceu com ENABLE + FORCE ROW LEVEL SECURITY na V1 e
-- ADD COLUMN nao mexe nisso — nada a re-habilitar aqui. O trigger
-- trg_app_user_touch (updated_at) tambem continua valendo para as colunas novas.
