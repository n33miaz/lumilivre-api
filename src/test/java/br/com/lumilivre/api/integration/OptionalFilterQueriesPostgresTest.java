package br.com.lumilivre.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.controller.BookController;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.ReaderRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.service.AppUserService;
import br.com.lumilivre.api.service.BookService;

/**
 * Todos os filtros opcionais da API contra Postgres de verdade — com o filtro
 * vazio e com cada campo preenchido.
 *
 * <p>Motivo: o padrão {@code (:p IS NULL OR campo = :p)} passa em H2 e pode
 * falhar no Postgres. Quando o parâmetro é nulo, o Hibernate manda
 * {@code setNull} com o tipo JDBC e o Postgres resolve; quando é <b>preenchido</b>
 * e temporal, o driver manda o valor sem OID e o Postgres não tem contexto para
 * inferir o tipo do {@code ? IS NULL} — daí o
 * {@code could not determine data type of parameter}. Foi assim que
 * {@code /api/books/advanced?publicationDate=...} respondia 500 no stack local
 * enquanto a suíte em H2 passava.
 *
 * <p>Este teste é a rede: qualquer troca de Hibernate, de driver ou de Postgres
 * que reintroduza o problema falha aqui, no CI, e não durante uma demonstração.
 * Skip silencioso quando o Docker não está acessível.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "jwt.secret=integration-test-secret-with-enough-length-for-hmac-signature-aaaaa",
        "supabase.url=http://localhost:9999",
        "supabase.key=test",
        "supabase.service-role.key=test",
        "lumilivre.storage.provider=local",
        "lumilivre.storage.local.base-dir=./build/storage-filters",
        "app.cors.allowed-origins=http://localhost:5173",
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class OptionalFilterQueriesPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lumilivre_filters")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    private static final Pageable PAGE = PageRequest.of(0, 10);
    private static final LocalDate DATE = LocalDate.of(2007, 4, 12);
    private static final OffsetDateTime FROM = OffsetDateTime.now().minusYears(5);
    private static final OffsetDateTime TO = OffsetDateTime.now().plusYears(1);

    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private ReaderRepository readerRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AcademicModuleRepository academicModuleRepository;
    @Autowired private StudyShiftRepository studyShiftRepository;
    @Autowired private BookService bookService;
    @Autowired private AppUserService appUserService;
    @Autowired private BookController bookController;
    @Autowired private CustomUserDetailsService userDetailsService;

    // ---- livros ---------------------------------------------------------------

    @Test
    void bookAdvancedSearchWithEveryFilterNull() {
        assertThatCode(() -> bookRepository.buscarAvancado(
                null, null, null, null, null, null, null, null, null, PAGE))
                .doesNotThrowAnyException();
    }

    /**
     * Regressão do 500 confirmado no stack local: com a data preenchida, o
     * {@code :dataLancamento IS NULL} sem cast fazia o Postgres responder
     * "could not determine data type of parameter $17".
     */
    @Test
    void bookAdvancedSearchWithPublicationDateFilled() {
        assertThatCode(() -> bookRepository.buscarAvancado(
                null, null, null, null, null, null, null, null, LocalDate.of(1899, 1, 1), PAGE))
                .doesNotThrowAnyException();
    }

    @Test
    void bookAdvancedSearchWithEveryFilterFilled() {
        assertThatCode(() -> bookRepository.buscarAvancado(
                "%dom%", "9788535902778", "%machado%", "%romance%", "%editora%",
                "869", AgeRating.GENERAL, CoverType.PAPERBACK, LocalDate.of(1899, 1, 1), PAGE))
                .doesNotThrowAnyException();
    }

    /**
     * O LEFT JOIN em generos multiplica as linhas: com {@code COUNT(e)} um livro
     * de 3 exemplares e 2 generos aparecia com 6 exemplares na tela do filtro
     * avancado, enquanto a listagem agrupada mostrava 3.
     */
    @Test
    void bookAdvancedSearchCountsCopiesNotCopiesTimesGenres() {
        var agrupado = bookRepository.findLivrosAgrupados(PageRequest.of(0, 50), null).getContent();
        var avancado = bookRepository.buscarAvancado(
                null, null, null, null, null, null, null, null, null, PageRequest.of(0, 50)).getContent();

        assertThat(agrupado).isNotEmpty();
        assertThat(avancado).isNotEmpty();

        int comparados = 0;
        for (var esperado : agrupado) {
            var encontrado = avancado.stream()
                    .filter(item -> item.getId().equals(esperado.getId()))
                    .findFirst();
            if (encontrado.isEmpty()) {
                continue;
            }
            assertThat(encontrado.get().getCopyCount())
                    .as("exemplares de %s", esperado.getTitle())
                    .isEqualTo(esperado.getCopyCount());
            comparados++;
        }

        // Sem isto o teste passaria de graca caso as duas listas nao se cruzassem.
        assertThat(comparados).as("livros comparados entre as duas listagens").isGreaterThan(5);
        // E ao menos um livro do seed tem mais de um genero — e o caso que
        // multiplicava a contagem.
        assertThat(agrupado.stream().anyMatch(item -> item.getCopyCount() > 1)).isTrue();
    }

    @Test
    void bookGroupedSearchWithNullAndFilledText() {
        assertThatCode(() -> {
            bookRepository.findLivrosAgrupados(PAGE, null);
            bookRepository.findLivrosAgrupados(PAGE, "");
            bookRepository.findLivrosAgrupados(PAGE, "dom");
        }).doesNotThrowAnyException();
    }

    @Test
    void bookReportWithEveryFilterNullAndFilled() {
        assertThatCode(() -> bookRepository.findForReport(
                null, null, null, null, null, null, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> bookRepository.findForReport(
                "%romance%", "%machado%", "%editora%", "869", "GENERAL", "PAPERBACK", FROM, TO))
                .doesNotThrowAnyException();
    }

    // ---- exemplares -----------------------------------------------------------

    @Test
    void bookCopyReportWithEveryFilterNullAndFilled() {
        assertThatCode(() -> bookCopyRepository.findForReport(null, null, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> bookCopyRepository.findForReport(
                BookCopyStatus.AVAILABLE, "%LUM%", FROM, TO)).doesNotThrowAnyException();
    }

    // ---- leitores -------------------------------------------------------------

    @Test
    void readerAdvancedSearchWithEveryFilterNull() {
        assertThatCode(() -> readerRepository.buscarAvancadoV2(
                null, null, null, null, null, null, null, null, null, PAGE))
                .doesNotThrowAnyException();
    }

    /**
     * A data de nascimento nunca é enviada pela rota atual (o controller passa
     * null), então o cast que a protege não tinha como ser exercitado por ali.
     * Aqui ela vai preenchida — é o mesmo formato que derrubava a busca de livros.
     */
    @Test
    void readerAdvancedSearchWithBirthDateFilled() {
        assertThatCode(() -> readerRepository.buscarAvancadoV2(
                null, null, null, null, null, null, DATE, null, null, PAGE))
                .doesNotThrowAnyException();
    }

    @Test
    void readerAdvancedSearchWithEveryFilterFilled() {
        assertThatCode(() -> readerRepository.buscarAvancadoV2(
                PenaltyCode.WARNING, "2024001", "%ana%", "%desenvolvimento%", 1, 1,
                DATE, "%example.com%", "11990010001", PAGE))
                .doesNotThrowAnyException();
    }

    @Test
    void readerRankingWithEveryFilterNullAndFilled() {
        assertThatCode(() -> readerRepository.findRankingItems(null, null, null, PAGE))
                .doesNotThrowAnyException();
        assertThatCode(() -> readerRepository.findRankingItems(1, 1, 1, PAGE))
                .doesNotThrowAnyException();
    }

    @Test
    void readerReportWithEveryFilterNullAndFilled() {
        assertThatCode(() -> readerRepository.findForReport(null, null, null, null, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> readerRepository.findForReport(
                1, 1, 1, PenaltyCode.WARNING, FROM, TO)).doesNotThrowAnyException();
    }

    // ---- empréstimos ----------------------------------------------------------

    @Test
    void loanAdvancedSearchWithEveryFilterNull() {
        assertThatCode(() -> loanRepository.searchAdvancedListItems(
                null, null, null, null, null, null, null, null, OffsetDateTime.now(), PAGE))
                .doesNotThrowAnyException();
    }

    @Test
    void loanAdvancedSearchWithEveryFilterFilled() {
        assertThatCode(() -> loanRepository.searchAdvancedListItems(
                "ACTIVE", "%LUM%", "%dom%", "%ana%", FROM, TO, FROM, TO,
                OffsetDateTime.now(), PAGE)).doesNotThrowAnyException();
    }

    @Test
    void loanReportWithEveryFilterNullAndFilled() {
        assertThatCode(() -> loanRepository.findForReport(null, null, null, null, null, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> loanRepository.findForReport(
                FROM, TO, LoanStatus.ACTIVE, "%2024001%", 1, "%LUM%", 1))
                .doesNotThrowAnyException();
    }

    // ---- usuários (Specification) --------------------------------------------

    /**
     * Regressão do 500 confirmado no stack local: o JPQL anterior comparava o
     * enum com LIKE ({@code lower(bytea)} → SQLState 42883) e deixava o UUID
     * solto num {@code :id IS NULL}. Qualquer combinação de filtros falhava.
     */
    @Test
    void userAdvancedSearchWithEveryCombinationOfFilters() {
        UUID admin = UUID.fromString("00000000-0000-4000-8000-000000001001");

        assertThatCode(() -> appUserService.searchUsersAdvanced(null, null, null, PAGE))
                .doesNotThrowAnyException();
        assertThatCode(() -> appUserService.searchUsersAdvanced(admin, null, null, PAGE))
                .doesNotThrowAnyException();
        assertThatCode(() -> appUserService.searchUsersAdvanced(null, "admin", null, PAGE))
                .doesNotThrowAnyException();
        assertThatCode(() -> appUserService.searchUsersAdvanced(null, null, Role.ADMIN, PAGE))
                .doesNotThrowAnyException();
        assertThatCode(() -> appUserService.searchUsersAdvanced(admin, "admin", Role.ADMIN, PAGE))
                .doesNotThrowAnyException();
    }

    @Test
    void userAdvancedSearchFiltersByRoleAndEmail() {
        var admins = appUserService.searchUsersAdvanced(null, null, Role.ADMIN, PAGE);
        assertThat(admins.getContent()).isNotEmpty()
                .allSatisfy(user -> assertThat(user.getRole()).isEqualTo(Role.ADMIN));

        var byEmail = appUserService.searchUsersAdvanced(null, "admin", null, PAGE);
        assertThat(byEmail.getContent()).isNotEmpty()
                .allSatisfy(user -> assertThat(user.getEmail()).isEqualTo("admin"));

        var noMatch = appUserService.searchUsersAdvanced(null, "ninguem@example.test", null, PAGE);
        assertThat(noMatch.getContent()).isEmpty();
    }

    // ---- dados de referência --------------------------------------------------

    @Test
    void referenceSummariesWithNullAndFilledText() {
        assertThatCode(() -> {
            courseRepository.findSummariesByFilter(null, PAGE);
            courseRepository.findSummariesByFilter("adm", PAGE);
            academicModuleRepository.findSummaries(null, PAGE);
            academicModuleRepository.findSummaries("1", PAGE);
            studyShiftRepository.findSummaries(null, PAGE);
            studyShiftRepository.findSummaries("mat", PAGE);
        }).doesNotThrowAnyException();
    }

    // ---- SEC-15: a query nativa e o sort --------------------------------------

    /**
     * Cada campo da allowlist é uma coluna real: se algum nome estiver errado, o
     * Postgres reclama aqui. É o teste que impede a allowlist de virar uma lista
     * de nomes bonitos que não ordenam nada.
     */
    @ParameterizedTest(name = "sort={0} ordena de verdade")
    @ValueSource(strings = {
            "status", "copyCode", "physicalLocation", "isbn", "deweyCode", "title", "author", "publisher"
    })
    void everyAllowedSortFieldIsAValidColumn(String field) {
        assertThatCode(() -> bookService.buscarParaListaAdmin(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, field))).getContent())
                .doesNotThrowAnyException();
    }

    @Test
    void allowedSortActuallyOrdersTheResult() {
        List<String> desc = bookService.buscarParaListaAdmin(
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "copyCode")))
                .getContent().stream().map(item -> item.getCopyCode()).toList();

        assertThat(desc).isNotEmpty().isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @ParameterizedTest(name = "sort={0} vira 400 antes de chegar no banco")
    @ValueSource(strings = {
            "id;DROP TABLE book--",
            "copy_code",
            "l.title",
            "password_hash",
            "coluna_inexistente"
    })
    void maliciousOrRawSortNeverReachesThePostgres(String field) {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> bookService.buscarParaListaAdmin(
                        PageRequest.of(0, 5, Sort.by(field))))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("error.sort.invalid-field"));

        // E o banco continua de pé — nada foi executado.
        assertThat(bookRepository.count()).isPositive();
    }

    // ---- recomendações: o único cache com chave por usuário -------------------
    // Moram aqui para reaproveitar este container e este seed em vez de subir um
    // terceiro contexto Postgres só para duas verificações.

    /**
     * A rota respondia 500 no stack local: o método navega até
     * {@code book.genres} (lazy) e, com {@code open-in-view=false}, a sessão já
     * estava fechada. Passa pelo proxy do Spring de propósito — é ele que abre a
     * transação declarada no serviço.
     */
    @Test
    void readerRecommendationsLoadLazyGenresInsideTheTransaction() {
        authenticateAs("2024001");
        try {
            assertThatCode(() -> bookController.recommendations("2024001", Locale.forLanguageTag("pt-BR")))
                    .doesNotThrowAnyException();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** Sem principal nenhum a rota nega — prova que method security está ativa. */
    @Test
    void recommendationsRequireAnAuthenticatedPrincipal() {
        SecurityContextHolder.clearContext();

        assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> bookController.recommendations("2024001", Locale.forLanguageTag("pt-BR")));
    }

    /**
     * O cache das recomendações tem a matrícula como chave. Se a rota aceitasse
     * qualquer matrícula, um LEITOR receberia a lista derivada do histórico de
     * outro — servida do cache, com toda a fidelidade. Quem fecha isso é o
     * {@code @CanAccessReader}.
     */
    @Test
    void readerCannotAskForAnotherReadersRecommendations() {
        authenticateAs("2024001");
        try {
            assertThatCode(() -> bookController.recommendations("2024001", Locale.forLanguageTag("pt-BR")))
                    .doesNotThrowAnyException();

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> bookController.recommendations("2024002", Locale.forLanguageTag("pt-BR")));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void staffStillReadsAnyReadersRecommendations() {
        authenticateAs("admin");
        try {
            assertThatCode(() -> bookController.recommendations("2024002", Locale.forLanguageTag("pt-BR")))
                    .doesNotThrowAnyException();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateAs(String login) {
        var details = userDetailsService.loadUserByUsername(login);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
