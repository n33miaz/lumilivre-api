package br.com.lumilivre.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test do novo baseline em ingles (PR 3).
 *
 * <p>Sobe um Postgres 16 via Testcontainers e aplica todas as migracoes de
 * {@code classpath:db/migration}. Valida:
 * <ul>
 *   <li>Todas as migracoes aplicam sem erro.</li>
 *   <li>O numero de migracoes aplicadas bate com o esperado.</li>
 *   <li>Nenhuma migracao falha (success=true).</li>
 * </ul>
 *
 * <p>Executa apenas quando Docker esta disponivel. Sem Docker, o teste e
 * pulado silenciosamente - CI precisa ter Docker ou usar runner com
 * Testcontainers Cloud.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lumilivre_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void applies_all_english_migrations_cleanly() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        MigrateResult result = flyway.migrate();

        assertThat(result.success)
                .as("Flyway should apply all migrations successfully")
                .isTrue();

        assertThat(result.migrationsExecuted)
                .as("Must apply V1..V8 core baseline")
                .isGreaterThanOrEqualTo(8);

        assertThat(result.warnings)
                .as("No warnings expected on a fresh database")
                .isEmpty();
    }

    @Test
    void migrations_are_idempotent_on_reapply() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        MigrateResult secondRun = flyway.migrate();

        assertThat(secondRun.migrationsExecuted)
                .as("Re-running migrate() on an up-to-date DB should be a no-op")
                .isZero();
    }

    @Test
    void optional_demo_seed_populates_business_data() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/seed")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(countRows("reader")).isEqualTo(50);
        assertThat(countRowsWhere("reader", "penalty_code IS NOT NULL")).isEqualTo(11);
        assertThat(countRowsWhere("library_settings", "library_type = 'SCHOOL'")).isEqualTo(1);
        assertThat(countRows("book")).isEqualTo(100);
        assertThat(countRowsWhere("book", "cover_url IS NOT NULL")).isEqualTo(92);
        assertThat(countRows("book_copy")).isEqualTo(150);
        assertThat(countRowsWhere("book_copy", "status = 'AVAILABLE'")).isEqualTo(98);
        assertThat(countRowsWhere("book_copy", "status = 'BORROWED'")).isEqualTo(30);
        assertThat(countRowsWhere("book_copy", "status = 'MAINTENANCE'")).isEqualTo(11);
        assertThat(countRowsWhere("book_copy", "status = 'UNAVAILABLE'")).isEqualTo(11);
        assertThat(countRows("loan")).isEqualTo(60);
        assertThat(countRowsWhere("loan", "status = 'ACTIVE'")).isEqualTo(24);
        assertThat(countRowsWhere("loan", "status = 'OVERDUE'")).isEqualTo(6);
        assertThat(countRowsWhere("loan", "status = 'COMPLETED'")).isEqualTo(30);
        assertThat(countRows("loan_request")).isEqualTo(15);
        assertThat(countRowsWhere("loan_request", "status = 'PENDING'")).isEqualTo(4);
        assertThat(countRowsWhere("loan_request", "status = 'ACCEPTED'")).isEqualTo(4);
        assertThat(countRowsWhere("loan_request", "status = 'REJECTED'")).isEqualTo(3);
        assertThat(countRowsWhere("loan_request", "status = 'CANCELLED'")).isEqualTo(4);
        assertThat(countRows("reservation")).isEqualTo(15);
        assertThat(countRowsWhere("reservation", "status = 'WAITING'")).isEqualTo(5);
        assertThat(countRowsWhere("reservation", "status = 'READY'")).isEqualTo(3);
        assertThat(countRowsWhere("reservation", "status = 'FULFILLED'")).isEqualTo(4);
        assertThat(countRowsWhere("reservation", "status = 'EXPIRED'")).isEqualTo(2);
        assertThat(countRows("app_content")).isEqualTo(14);
        assertThat(countRowsWhere("app_content", "content_type = 'WORK'")).isEqualTo(4);
        assertThat(countRowsWhere("app_content", "is_published = TRUE")).isEqualTo(13);
        assertThat(countRows("access_log")).isEqualTo(50);
        assertThat(countRows("audit_log")).isEqualTo(15);
        assertThat(countRowsWhere("outbox_event", "status = 'SENT'")).isEqualTo(3);

        assertThat(metric("active_loans")).isEqualTo(24);
        assertThat(metric("overdue_loans")).isEqualTo(6);
        assertThat(metric("pending_requests")).isEqualTo(4);
        assertThat(metric("avg_return_days")).isGreaterThan(0);
    }

    private long countRows(String tableName) throws Exception {
        return countRowsWhere(tableName, "TRUE");
    }

    private long countRowsWhere(String tableName, String whereClause) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private double metric(String columnName) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT " + columnName + " FROM mv_dashboard_stats")) {
            resultSet.next();
            return resultSet.getDouble(1);
        }
    }
}
