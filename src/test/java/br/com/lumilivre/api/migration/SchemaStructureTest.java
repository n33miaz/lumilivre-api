package br.com.lumilivre.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifica caracteristicas estruturais do baseline em ingles:
 * existencia das tabelas e views, extensoes, indices chave, RLS e
 * constraints que precisam sobreviver a refatoracoes futuras.
 *
 * <p>Diferente de {@link FlywayMigrationTest}, aqui nao olhamos para o
 * resultado do Flyway e sim para o estado final do schema. Se alguem
 * renomear uma tabela ou remover um indice critico, este teste quebra.
 */
@Testcontainers(disabledWithoutDocker = true)
class SchemaStructureTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lumilivre_schema_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void applyMigrations() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void all_core_tables_exist_in_english() throws Exception {
        List<String> tables = listObjects(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'");

        assertThat(tables).contains(
                "course", "academic_module", "study_shift", "genre", "dewey_classification",
                "reader", "app_user",
                "book", "book_genre", "book_copy",
                "loan", "loan_request", "reservation",
                "app_content", "password_reset_token",
                "outbox_event", "audit_log"
        );

        // thesis foi generalizado em app_content e nao existe mais
        assertThat(tables).doesNotContain("thesis");

        // Nao deve existir nada em portugues remanescente
        assertThat(tables).doesNotContain(
                "leitor", "usuario", "curso", "modulo", "turno", "genero",
                "livro", "exemplar", "emprestimo", "solicitacao_emprestimo",
                "reserva", "tcc", "token_reset_senha", "cdd_classificacao"
        );
    }

    @Test
    void required_extensions_are_installed() throws Exception {
        List<String> extensions = listObjects("SELECT extname FROM pg_extension");
        assertThat(extensions).contains("pgcrypto", "citext", "pg_trgm", "unaccent");
    }

    @Test
    void dashboard_materialized_views_exist_in_english() throws Exception {
        List<String> views = listObjects(
                "SELECT matviewname FROM pg_matviews WHERE schemaname = 'public'");

        assertThat(views).contains("mv_dashboard_stats", "mv_top_books", "mv_loans_by_month");
        assertThat(views).doesNotContain("mv_top_livros", "mv_emprestimos_por_mes");
    }

    @Test
    void business_primary_keys_use_uuid_strategy() throws Exception {
        List<String> uuidTables = listObjects(
                "SELECT table_name FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND column_name = 'id' AND data_type = 'uuid'");

        assertThat(uuidTables).contains(
                "reader", "app_user", "book", "book_copy",
                "loan", "loan_request", "reservation", "app_content"
        );
    }

    @Test
    void trigram_and_fts_indexes_exist_on_search_hotspots() throws Exception {
        List<String> indexes = listObjects(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'");

        assertThat(indexes).contains(
                "idx_reader_full_name_trgm",
                "idx_book_title_trgm",
                "idx_book_author_trgm",
                "idx_book_fts"
        );
    }

    @Test
    void active_loan_composite_indexes_exist() throws Exception {
        List<String> indexes = listObjects(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'");

        assertThat(indexes).contains(
                "idx_loan_reader_id_status_due_at",
                "idx_loan_book_copy_id_status",
                "idx_loan_status_due_at",
                "idx_reservation_book_id_status_queue_position"
        );
    }

    @Test
    void partial_unique_index_prevents_duplicate_active_reservation() throws Exception {
        try (Connection conn = newConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT indexdef FROM pg_indexes " +
                     "WHERE schemaname = 'public' AND indexname = 'uq_reservation_active_reader_book'")) {
            assertThat(rs.next())
                    .as("uq_reservation_active_reader_book must exist")
                    .isTrue();
            String def = rs.getString("indexdef");
            assertThat(def).contains("UNIQUE").contains("WHERE").containsAnyOf("WAITING", "READY");
        }
    }

    @Test
    void app_user_carries_account_status_and_revocation_columns() throws Exception {
        // V7: sem estas colunas o login volta a ignorar desligamento e o JWT
        // volta a ser irrevogável.
        List<String> columns = listObjects(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = 'app_user'");

        assertThat(columns).contains("active", "locked", "token_version");
        // O corte por timestamp foi substituído pelo contador: comparar iat com
        // um instante deixava 1s de janela em que o logout não revogava.
        assertThat(columns).doesNotContain("tokens_valid_from");
    }

    @Test
    void account_status_columns_are_not_null_with_safe_defaults() throws Exception {
        try (Connection conn = newConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT column_name, is_nullable, column_default " +
                     "FROM information_schema.columns " +
                     "WHERE table_schema = 'public' AND table_name = 'app_user' " +
                     "AND column_name IN ('active', 'locked', 'token_version')")) {
            int checked = 0;
            while (rs.next()) {
                String column = rs.getString("column_name");
                assertThat(rs.getString("is_nullable"))
                        .as("%s must be NOT NULL", column)
                        .isEqualTo("NO");
                assertThat(rs.getString("column_default"))
                        .as("%s must keep existing rows working", column)
                        .isNotNull();
                checked++;
            }
            assertThat(checked).isEqualTo(3);
        }
    }

    @Test
    void token_version_cannot_go_backwards() throws Exception {
        // Contador andando para tras faria tokens antigos voltarem a valer.
        List<String> constraints = listObjects(
                "SELECT conname FROM pg_constraint c " +
                "JOIN pg_class t ON t.oid = c.conrelid " +
                "WHERE t.relname = 'app_user' AND c.contype = 'c'");

        assertThat(constraints).contains("ck_app_user_token_version_nonneg");
    }

    @Test
    void partial_indexes_support_account_status_lookups() throws Exception {
        List<String> indexes = listObjects(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'");

        assertThat(indexes).contains("idx_app_user_role_active", "idx_app_user_reader_id_alive");
    }

    @Test
    void rls_is_enabled_on_all_business_tables() throws Exception {
        List<String> rlsOff = listObjects(
                "SELECT c.relname FROM pg_class c " +
                "JOIN pg_namespace n ON c.relnamespace = n.oid " +
                "WHERE n.nspname = 'public' AND c.relkind = 'r' " +
                "AND c.relname <> 'flyway_schema_history' " +
                "AND c.relrowsecurity = false");

        assertThat(rlsOff)
                .as("Every business table must have RLS enabled")
                .isEmpty();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Connection newConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static List<String> listObjects(String sql) throws Exception {
        List<String> results = new ArrayList<>();
        try (Connection conn = newConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(rs.getString(1));
            }
        }
        return results;
    }
}
