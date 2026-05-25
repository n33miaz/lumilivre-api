-- =============================================================================
--  LumiLivre — Postgres init script (local dev)
-- -----------------------------------------------------------------------------
--  Roda uma única vez no primeiro start do container (pasta /docker-entrypoint-
--  initdb.d). Apenas habilita as extensions exigidas pelas migrations Flyway.
--  O schema completo (tabelas, índices, views) é aplicado por:
--      LUMILIVRE_FLYWAY_ENABLED=true ./mvnw spring-boot:run
--  Mantém paridade com Supabase: pgcrypto/citext/pg_trgm/unaccent já provisionados.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS citext   WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_trgm  WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;
