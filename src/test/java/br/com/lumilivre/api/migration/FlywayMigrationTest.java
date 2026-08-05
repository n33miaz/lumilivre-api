package br.com.lumilivre.api.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
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
                .as("Must apply the whole baseline (V1..V9)")
                .isGreaterThanOrEqualTo(9);

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
    void latest_migration_applies_on_a_database_already_at_the_previous_version() {
        // Cenário de deploy real: o banco está na versão anterior e só a última
        // chega. Não basta aplicar em banco novo — ADD COLUMN tem de passar em
        // tabela com dados. Ao acrescentar uma migration, mova o `target` para a
        // penúltima versão e mantenha o `isEqualTo(1)`.
        Flyway upToPrevious = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("8"))
                .cleanDisabled(false)
                .load();

        upToPrevious.clean();
        assertThat(upToPrevious.migrate().success).isTrue();

        Flyway toLatest = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = toLatest.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted)
                .as("Only the pending migrations should run on an existing database")
                .isEqualTo(1);
        assertThat(result.warnings).isEmpty();
    }

    /**
     * O caso que o projeto realmente vive: as versões são reservadas por tarefa
     * antes de o arquivo existir, e as tarefas não terminam na ordem da reserva —
     * a V9 (acesso de convidado) foi escrita e aplicada antes da V8 (interesse).
     *
     * <p>Num banco que já está na V9, o Flyway considera a V8 "resolvida e não
     * aplicada" e, por default, recusa: {@code validateOnMigrate} roda antes de
     * qualquer migração, então a API inteira não sobe. Este teste trava as duas
     * pontas — que sem {@code out-of-order} realmente falha, e que com ele a V8
     * entra sozinha e cria a tabela.
     */
    @Test
    void the_late_reserved_version_still_applies_on_a_database_already_ahead() throws Exception {
        Flyway everything = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        everything.clean();
        assertThat(everything.migrate().success).isTrue();

        // Reproduz o banco de dev/produção como ele ficou: histórico com a V9 e
        // sem a V8, porque a V8 não existia quando a V9 foi aplicada.
        execute("DROP TABLE book_interest");
        execute("DELETE FROM flyway_schema_history WHERE version = '8'");

        Flyway inOrderOnly = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .outOfOrder(false)
                .load();
        assertThatThrownBy(inOrderOnly::migrate)
                .as("without out-of-order Flyway refuses and the API does not boot")
                .isInstanceOf(FlywayException.class);

        Flyway allowingOutOfOrder = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .outOfOrder(true)
                .load();
        MigrateResult result = allowingOutOfOrder.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted)
                .as("only the version that was missing should run")
                .isEqualTo(1);
        assertThat(countRows("book_interest")).isZero();
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

    private void execute(String sql) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
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
