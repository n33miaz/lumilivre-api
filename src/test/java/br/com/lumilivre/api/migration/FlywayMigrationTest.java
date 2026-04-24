package br.com.lumilivre.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

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
                .as("Must apply V1..V5 core baseline")
                .isGreaterThanOrEqualTo(5);

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
}
