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
        assertThat(countRows("book")).isEqualTo(103);
        assertThat(countRowsWhere("book", "cover_url IS NOT NULL")).isEqualTo(95);
        assertThat(countRows("book_copy")).isEqualTo(161);
        assertThat(countRowsWhere("book_copy", "status = 'AVAILABLE'")).isEqualTo(102);
        assertThat(countRowsWhere("book_copy", "status = 'BORROWED'")).isEqualTo(36);
        assertThat(countRowsWhere("book_copy", "status = 'MAINTENANCE'")).isEqualTo(12);
        assertThat(countRowsWhere("book_copy", "status = 'UNAVAILABLE'")).isEqualTo(11);
        assertThat(countRows("loan")).isEqualTo(164);
        assertThat(countRowsWhere("loan", "status = 'ACTIVE'")).isEqualTo(29);
        assertThat(countRowsWhere("loan", "status = 'OVERDUE'")).isEqualTo(7);
        assertThat(countRowsWhere("loan", "status = 'COMPLETED'")).isEqualTo(128);
        assertThat(countRows("loan_request")).isEqualTo(17);
        assertThat(countRowsWhere("loan_request", "status = 'PENDING'")).isEqualTo(5);
        assertThat(countRowsWhere("loan_request", "status = 'ACCEPTED'")).isEqualTo(4);
        assertThat(countRowsWhere("loan_request", "status = 'REJECTED'")).isEqualTo(4);
        assertThat(countRowsWhere("loan_request", "status = 'CANCELLED'")).isEqualTo(4);
        assertThat(countRows("reservation")).isEqualTo(18);
        assertThat(countRowsWhere("reservation", "status = 'WAITING'")).isEqualTo(8);
        assertThat(countRowsWhere("reservation", "status = 'READY'")).isEqualTo(3);
        assertThat(countRowsWhere("reservation", "status = 'FULFILLED'")).isEqualTo(4);
        assertThat(countRowsWhere("reservation", "status = 'EXPIRED'")).isEqualTo(2);
        assertThat(countRows("app_content")).isEqualTo(17);
        assertThat(countRowsWhere("app_content", "content_type = 'WORK'")).isEqualTo(5);
        assertThat(countRowsWhere("app_content", "is_published = TRUE")).isEqualTo(16);
        assertThat(countRows("access_log")).isEqualTo(90);
        assertThat(countRows("audit_log")).isEqualTo(37);
        assertThat(countRowsWhere("outbox_event", "status = 'SENT'")).isEqualTo(5);

        assertThat(metric("active_loans")).isEqualTo(29);
        assertThat(metric("overdue_loans")).isEqualTo(7);
        assertThat(metric("pending_requests")).isEqualTo(5);
        assertThat(metric("avg_return_days")).isGreaterThan(0);
    }

    /**
     * O que o seed tem de <b>provar</b>, e não apenas contar.
     *
     * <p>Cada assertiva aqui corresponde a uma tela que ficaria vazia sem a
     * linha correspondente. O caso que motivou o teste é o primeiro: até este
     * seed <b>nenhum</b> dos livros estava sem exemplar, então
     * {@code ?unmetOnly=true} abria sem nada e o indicador de interesse não
     * tinha como demonstrar para que serve. Um caminho que o seed não exercita é
     * um caminho que ninguém olha.
     */
    @Test
    void demo_seed_covers_the_paths_that_have_no_other_evidence() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/seed")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        assertThat(flyway.migrate().success).isTrue();

        // As duas formas de "não temos disponível", que são coisas diferentes na
        // tela: sem exemplar nenhum cadastrado e com todo o acervo emprestado.
        assertThat(countRowsWhere("book",
                "NOT EXISTS (SELECT 1 FROM book_copy c WHERE c.book_id = book.id)"))
                .as("livro sem exemplar cadastrado")
                .isEqualTo(1);
        assertThat(countRowsWhere("book",
                "EXISTS (SELECT 1 FROM book_copy c WHERE c.book_id = book.id)"
                        + " AND NOT EXISTS (SELECT 1 FROM book_copy c WHERE c.book_id = book.id"
                        + " AND c.status = 'AVAILABLE')"))
                .as("livros com acervo mas sem exemplar livre")
                .isGreaterThanOrEqualTo(11);

        // Interesse concentrado: se todo mundo curtisse coisas diferentes, o
        // interestCount nunca passaria de 1 e a ordenação do resumo não mostraria
        // nada. O topo tem de ser um livro que a biblioteca não consegue atender.
        assertThat(countRows("book_interest")).isEqualTo(138);
        assertThat(distinctCount("book_interest", "reader_id"))
                .as("interesse espalhado entre leitores, senão o interestCount nunca passa de 1")
                .isEqualTo(24);
        assertThat(topUnmetInterestCount())
                .as("o livro mais desejado sem exemplar disponível")
                .isEqualTo(18);

        // Ciclo de vida de conta (V7): os dois estados são independentes e a tela
        // de admin tem um toggle para cada.
        assertThat(countRowsWhere("app_user", "active = FALSE")).isEqualTo(1);
        assertThat(countRowsWhere("app_user", "locked = TRUE")).isEqualTo(1);
        assertThat(countRowsWhere("app_user", "role = 'ADMIN' AND active AND NOT locked"))
                .as("o seed nunca pode deixar o ambiente sem ADMIN utilizável")
                .isEqualTo(1);
        assertThat(countRowsWhere("app_user", "reader_id IS NOT NULL"))
                .as("logins de aluno: um só não demonstra que o interesse é por pessoa")
                .isEqualTo(9);
        assertThat(distinctCount("app_user", "preferred_locale"))
                .as("os cinco idiomas suportados aparecem em alguma conta")
                .isEqualTo(5);

        // Trilha de acesso (V9): sem alvo, "acesso negado" não diz a quê.
        assertThat(countRowsWhere("access_log", "event = 'BOOK_VIEWED' AND target_id IS NOT NULL"))
                .isPositive();
        assertThat(countRowsWhere("access_log", "event = 'CONTENT_VIEWED' AND target_id IS NOT NULL"))
                .isPositive();
        assertThat(countRowsWhere("access_log", "event = 'CATALOG_SEARCH'")).isPositive();
        assertThat(countRowsWhere("access_log", "event = 'ACCESS_DENIED' AND target_id IS NOT NULL"))
                .isPositive();

        // Auditoria: a metade que interessa numa revisão de segurança é a que não
        // deu certo.
        assertThat(countRowsWhere("audit_log", "result = 'FAILURE'")).isGreaterThanOrEqualTo(6);
        assertThat(countRowsWhere("audit_log", "result = 'DENIED'")).isGreaterThanOrEqualTo(3);
        assertThat(countRowsWhere("outbox_event", "event_type = 'PASSWORD_RESET'")).isEqualTo(1);

        // A flag do V9 vem escrita, e não herdada do DEFAULT.
        assertThat(countRowsWhere("library_settings", "guest_access_enabled = TRUE")).isEqualTo(1);
    }

    /**
     * O seed desliga RLS para inserir. Se o bloco final não rodasse, o banco
     * ficaria com as tabelas de negócio abertas e ninguém perceberia — nenhuma
     * consulta falha por RLS estar <i>desligada</i>.
     */
    @Test
    void demo_seed_leaves_row_level_security_back_on_every_table() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/seed")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        assertThat(flyway.migrate().success).isTrue();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     SELECT c.relname FROM pg_class c
                     JOIN pg_namespace n ON n.oid = c.relnamespace
                     WHERE n.nspname = 'public' AND c.relkind = 'r'
                       AND NOT (c.relrowsecurity AND c.relforcerowsecurity)
                     ORDER BY c.relname
                     """)) {
            var unprotected = new java.util.ArrayList<String>();
            while (resultSet.next()) {
                unprotected.add(resultSet.getString(1));
            }
            assertThat(unprotected)
                    .as("toda tabela tem de sair do seed com RLS habilitada e forçada")
                    .containsExactly("flyway_schema_history");
        }
    }

    /**
     * O {@code R__} re-roda a cada mudança de checksum e o dono roda o stack
     * local várias vezes por dia: duplicar aqui significaria demonstração com
     * dado repetido e, no pior caso, violação de constraint no boot.
     */
    @Test
    void demo_seed_is_idempotent_when_applied_twice() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration", "classpath:db/seed")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        assertThat(flyway.migrate().success).isTrue();

        String[] tables = {"reader", "app_user", "book", "book_genre", "book_copy", "book_interest",
                "loan", "loan_request", "reservation", "app_content", "access_log", "audit_log",
                "outbox_event"};
        var before = new java.util.LinkedHashMap<String, Long>();
        for (String table : tables) {
            before.put(table, countRows(table));
        }

        // O R__ só re-executa quando o checksum muda. Apagar a linha dele do
        // histórico é o que faz o Flyway aplicar o mesmo script de novo, pelo
        // executor de verdade — reproduz a re-execução sem editar o arquivo.
        execute("DELETE FROM flyway_schema_history WHERE script LIKE '%R__seed_demo_data%'");
        MigrateResult second = flyway.migrate();
        assertThat(second.success).isTrue();
        assertThat(second.migrationsExecuted).as("o seed repetível roda de novo").isEqualTo(1);

        for (String table : tables) {
            assertThat(countRows(table)).as("%s após a segunda aplicação", table)
                    .isEqualTo(before.get(table));
        }
    }

    private long distinctCount(String tableName, String columnName) throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery(
                     "SELECT COUNT(DISTINCT " + columnName + ") FROM " + tableName)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private long topUnmetInterestCount() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     SELECT COUNT(DISTINCT i.id) AS wanted
                     FROM book_interest i
                     JOIN book b ON b.id = i.book_id
                     LEFT JOIN book_copy c ON c.book_id = b.id
                     GROUP BY b.id
                     HAVING COUNT(DISTINCT c.id) FILTER (WHERE c.status = 'AVAILABLE') = 0
                     ORDER BY wanted DESC
                     LIMIT 1
                     """)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
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
