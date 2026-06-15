-- =============================================================================
--  V6__add_outbox_locale.sql
-- -----------------------------------------------------------------------------
--  Persiste o locale (BCP-47) do destinatario no evento de outbox, capturado no
--  momento da publicacao. O envio assincrono passa a renderizar o cabecalho e o
--  rodape do e-mail no mesmo idioma do assunto/corpo ja resolvidos (ADR-009).
--  Coluna anulavel: eventos antigos caem no locale padrao (pt-BR) no envio.
-- =============================================================================

ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS locale VARCHAR(10);

COMMENT ON COLUMN outbox_event.locale IS
    'BCP-47 language tag captured at publish time so the async sender renders the email shell in the recipient locale (ADR-009).';
