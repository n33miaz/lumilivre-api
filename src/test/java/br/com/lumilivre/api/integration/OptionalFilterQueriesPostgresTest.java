package br.com.lumilivre.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

import jakarta.persistence.EntityManagerFactory;

import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.controller.BookController;
import br.com.lumilivre.api.controller.BookInterestController;
import br.com.lumilivre.api.dto.book.BookCopyCounts;
import br.com.lumilivre.api.dto.book.BookInterestSummaryResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookInterestRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.ReaderRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.security.CustomUserDetails;
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
        "spring.jpa.hibernate.ddl-auto=validate",
        // Contador de statements do Hibernate: e o que permite afirmar "uma
        // consulta so" num teste, em vez de conferir no log de SQL uma vez e
        // confiar que ninguem regride depois.
        "spring.jpa.properties.hibernate.generate_statistics=true"
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
    @Autowired private BookInterestController interestController;
    @Autowired private BookInterestRepository bookInterestRepository;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

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

    // ---- interesse (V8): a tabela nova, contra Postgres de verdade ------------
    // Aqui em vez de num contexto proprio pelo mesmo motivo das recomendacoes:
    // reaproveitar este container e este seed. A V8 nao semeia nada, entao o
    // deleteAll() de cada teste apaga apenas o que os testes daqui criaram.

    /**
     * O que a unicidade da V8 promete, verificado contra a constraint de verdade:
     * marcar duas vezes nao cria duas linhas e nao estoura. Com mock isso passaria
     * de qualquer forma — quem recusa a segunda linha e o Postgres.
     */
    @Test
    void markingInterestTwiceCreatesOneRowAndAnswersTheSameState() {
        bookInterestRepository.deleteAll();
        UUID bookId = anyBook().getId();
        authenticateAs("2024001");
        try {
            var first = interestController.toggle(bookId, PT_BR).getBody();
            var second = interestController.toggle(bookId, PT_BR).getBody();

            assertThat(first).isNotNull();
            assertThat(first.interested()).isTrue();
            assertThat(second).isEqualTo(first);
            assertThat(bookInterestRepository.count()).isEqualTo(1);
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    @Test
    void removingInterestIsIdempotentToo() {
        bookInterestRepository.deleteAll();
        UUID bookId = anyBook().getId();
        authenticateAs("2024001");
        try {
            interestController.toggle(bookId, PT_BR);
            assertThat(interestController.remove(bookId, PT_BR).getBody().interested()).isFalse();
            assertThat(interestController.remove(bookId, PT_BR).getBody().interested()).isFalse();
            assertThat(bookInterestRepository.count()).isZero();
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /**
     * IDOR: nao ha parametro de leitor em rota nenhuma, entao o teste verifica a
     * consequencia — o que um leitor marca nao aparece na lista do outro, e
     * desmarcar nao alcanca a linha de outra pessoa.
     */
    @Test
    void oneReadersInterestIsInvisibleToAnother() {
        bookInterestRepository.deleteAll();
        UUID bookId = anyBook().getId();
        try {
            authenticateAsReader("2024001");
            interestController.toggle(bookId, PT_BR);
            assertThat(interestController.mine(PAGE, PT_BR).getBody().getContent())
                    .extracting(item -> item.book().getId())
                    .containsExactly(bookId);

            authenticateAsReader("2024002");
            assertThat(interestController.mine(PAGE, PT_BR).getBody().getContent()).isEmpty();
            // Desmarcar como o outro leitor responde "nao interessado" para ele e
            // nao toca na linha do primeiro.
            assertThat(interestController.remove(bookId, PT_BR).getBody().interested()).isFalse();
            assertThat(bookInterestRepository.count()).isEqualTo(1);

            authenticateAsReader("2024001");
            assertThat(interestController.mine(PAGE, PT_BR).getBody().getContent()).hasSize(1);
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /** Interesse e do leitor: a equipe nao marca, nem le a lista de ninguem. */
    @Test
    void staffCannotMarkInterestNorReadAReadersList() {
        UUID bookId = anyBook().getId();
        authenticateAs("admin");
        try {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> interestController.toggle(bookId, PT_BR));
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> interestController.mine(PAGE, PT_BR));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void readerCannotReadTheLibraryIndicator() {
        authenticateAs("2024001");
        try {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> interestController.summary(false, PAGE, PT_BR));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * O cruzamento que decide compra de acervo: "quantos querem" x "quantos
     * exemplares disponiveis", numa consulta so. O livro sem exemplar disponivel e
     * com mais interesse tem de vir primeiro.
     */
    @Test
    void theSummaryCrossesInterestWithAvailabilityAndPutsUnmetDemandFirst() {
        bookInterestRepository.deleteAll();
        Book wanted = bookWithoutAvailableCopy();
        Book served = bookWithAvailableCopies();
        try {
            authenticateAsReader("2024001");
            interestController.toggle(wanted.getId(), PT_BR);
            interestController.toggle(served.getId(), PT_BR);
            authenticateAsReader("2024002");
            interestController.toggle(wanted.getId(), PT_BR);

            authenticateAs("admin");
            List<BookInterestSummaryResponse> rows =
                    interestController.summary(false, PAGE, PT_BR).getBody().getContent();

            assertThat(rows).hasSize(2);
            BookInterestSummaryResponse first = rows.get(0);
            assertThat(first.bookId()).isEqualTo(wanted.getId());
            assertThat(first.interestCount()).isEqualTo(2);
            assertThat(first.availableCopies()).isZero();
            // Tem exemplar, so nao tem exemplar livre: e a diferenca entre as duas
            // contagens que da sentido ao indicador.
            assertThat(first.totalCopies()).isPositive();

            BookInterestSummaryResponse second = rows.get(1);
            assertThat(second.bookId()).isEqualTo(served.getId());
            assertThat(second.interestCount()).isEqualTo(1);
            assertThat(second.availableCopies()).isPositive();
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /**
     * O LEFT JOIN com exemplares multiplica as linhas de interesse. Sem o
     * {@code COUNT(DISTINCT)} o painel diria que 3 alunos querem um livro que so
     * um aluno curtiu, porque ele tem 3 exemplares — o mesmo defeito que a
     * buscarAvancado ja teve com exemplar x genero.
     */
    @Test
    void theSummaryDoesNotCountInterestTimesCopies() {
        bookInterestRepository.deleteAll();
        Book withSeveralCopies = bookWithAtLeastTwoCopies();
        long realCopies = bookCopyRepository.countByBook_Id(withSeveralCopies.getId());
        try {
            authenticateAs("2024001");
            interestController.toggle(withSeveralCopies.getId(), PT_BR);

            authenticateAs("admin");
            BookInterestSummaryResponse row =
                    interestController.summary(false, PAGE, PT_BR).getBody().getContent().get(0);

            assertThat(row.interestCount()).isEqualTo(1);
            assertThat(row.totalCopies()).isEqualTo(realCopies);
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    @Test
    void unmetOnlyKeepsOnlyWhatTheLibraryCannotServe() {
        bookInterestRepository.deleteAll();
        Book wanted = bookWithoutAvailableCopy();
        Book served = bookWithAvailableCopies();
        try {
            authenticateAs("2024001");
            interestController.toggle(wanted.getId(), PT_BR);
            interestController.toggle(served.getId(), PT_BR);

            authenticateAs("admin");
            var page = interestController.summary(true, PAGE, PT_BR).getBody();

            assertThat(page.getContent()).extracting(BookInterestSummaryResponse::bookId)
                    .containsExactly(wanted.getId());
            // O total da pagina tem de respeitar o mesmo filtro; contar o conjunto
            // inteiro faria a paginacao prometer paginas que nao existem.
            assertThat(page.getTotalElements()).isEqualTo(1);
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /**
     * O caso que o T05 nao conseguiu testar porque nao existia no seed: livro com
     * <b>zero exemplares cadastrados</b>.
     *
     * <p>Nao e a mesma coisa que "todos emprestados", e a diferenca importa: o
     * LEFT JOIN com exemplares nao produz nenhuma linha, entao a agregacao tinha
     * de ser conferida contra um livro assim para provar que responde 0/0 em vez
     * de sumir da pagina. E o argumento de compra de acervo na forma mais direta
     * — "N alunos querem, nao temos nenhum".
     */
    @Test
    void theSummaryReportsBooksTheLibraryDoesNotOwnAtAll() {
        bookInterestRepository.deleteAll();
        Book missing = bookWithNoCopyAtAll();
        try {
            authenticateAsReader("2024001");
            interestController.toggle(missing.getId(), PT_BR);
            authenticateAsReader("2024002");
            interestController.toggle(missing.getId(), PT_BR);

            authenticateAs("admin");
            var page = interestController.summary(true, PAGE, PT_BR).getBody();

            assertThat(page.getContent()).hasSize(1);
            BookInterestSummaryResponse row = page.getContent().get(0);
            assertThat(row.bookId()).isEqualTo(missing.getId());
            assertThat(row.interestCount()).isEqualTo(2);
            assertThat(row.totalCopies()).isZero();
            assertThat(row.availableCopies()).isZero();
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /**
     * Uma pagina de interesses tem de custar uma consulta de dados, e nao uma por
     * livro.
     *
     * <p>O log de SQL do Postgres mostrou o N+1 de verdade aqui: {@code Book}
     * tem {@code deweyClassification} como {@code @ManyToOne}, cujo default no
     * JPA e EAGER, entao cada livro da pagina disparava um
     * {@code select ... from dewey_classification} proprio. O remedio foi trazer
     * o CDD no mesmo fetch; este teste e o que impede alguem de remover o
     * {@code LEFT JOIN FETCH} sem nada quebrar.
     */
    @Test
    void aPageOfInterestsCostsOneQueryAndNotOnePerBook() {
        bookInterestRepository.deleteAll();
        List<Book> books = bookRepository.findAll(PageRequest.of(0, 5)).getContent();
        try {
            authenticateAsReader("2024001");
            books.forEach(book -> interestController.toggle(book.getId(), PT_BR));

            Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            stats.clear();
            var page = interestController.mine(PageRequest.of(0, 10), PT_BR).getBody();

            assertThat(page.getContent()).hasSize(5);
            // Duas: a pagina e o count do Page. Com o N+1 seriam sete.
            assertThat(stats.getPrepareStatementCount())
                    .as("uma consulta de dados + o count da pagina")
                    .isLessThanOrEqualTo(2);
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /**
     * O resumo cruza interesse com disponibilidade para uma pagina inteira sem
     * uma contagem por livro — o N+1 classico deste tipo de painel.
     */
    @Test
    void theSummaryCrossesAWholePageInASingleQuery() {
        bookInterestRepository.deleteAll();
        List<Book> books = bookRepository.findAll(PageRequest.of(0, 8)).getContent();
        try {
            authenticateAsReader("2024001");
            books.forEach(book -> interestController.toggle(book.getId(), PT_BR));

            authenticateAs("admin");
            Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            stats.clear();
            var page = interestController.summary(false, PageRequest.of(0, 20), PT_BR).getBody();

            assertThat(page.getContent()).hasSize(8);
            assertThat(stats.getPrepareStatementCount())
                    .as("agregacao e count, sem uma contagem de exemplares por livro")
                    .isLessThanOrEqualTo(2);
        } finally {
            SecurityContextHolder.clearContext();
            bookInterestRepository.deleteAll();
        }
    }

    /**
     * Sort do cliente nao chega nas duas consultas de interesse: elas tem
     * ORDER BY proprio e em JPQL o Spring Data anexaria o campo do cliente ao
     * final, o que daria 500.
     */
    @Test
    void interestRoutesSurviveAnySortTheClientSends() {
        Pageable bogusSort = PageRequest.of(0, 10, Sort.by("naoExisteEmLugarNenhum"));
        authenticateAs("2024001");
        try {
            assertThatCode(() -> interestController.mine(bogusSort, PT_BR)).doesNotThrowAnyException();
        } finally {
            SecurityContextHolder.clearContext();
        }
        authenticateAs("admin");
        try {
            assertThatCode(() -> interestController.summary(false, bogusSort, PT_BR))
                    .doesNotThrowAnyException();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ---- navegacao por genero: ordem total e sort invalido --------------------

    /**
     * A rota respondia 500 para {@code ?sort=campoInexistente}: a allowlist do
     * SEC-15 cobria a query nativa da lista administrativa, e esta e JPQL. Contra
     * Postgres de verdade porque o erro nascia no Hibernate/banco, nao no Java.
     */
    @Test
    void genreBrowsingTurnsAnUnknownSortIntoFourHundredAndNotFiveHundred() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> bookService.buscarPorGenero(
                        "Romance", PageRequest.of(0, 5, Sort.by("campoInexistente"))))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("error.sort.invalid-field"));

        assertThat(bookRepository.count()).isPositive();
    }

    @ParameterizedTest(name = "genero ordenado por {0} chega no banco")
    @ValueSource(strings = {"title", "author", "rating", "publicationDate", "id"})
    void everyAllowedGenreSortFieldIsARealProperty(String field) {
        assertThatCode(() -> bookService.buscarPorGenero(
                "Romance", PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, field))).getContent())
                .doesNotThrowAnyException();
    }

    /**
     * Paginar sem ordem total repete ou pula linhas. Duas paginas de tamanho 1
     * sobre o mesmo genero nao podem devolver o mesmo livro.
     */
    @Test
    void genreBrowsingPaginatesWithoutRepeatingBooks() {
        String genre = "Romance";
        List<UUID> firstPages = new java.util.ArrayList<>();
        for (int page = 0; page < 4; page++) {
            bookService.buscarPorGenero(genre, PageRequest.of(page, 1)).getContent()
                    .forEach(card -> firstPages.add(card.getId()));
        }

        assertThat(firstPages).doesNotHaveDuplicates();
    }

    /** O card das listas carrega updatedAt: sem ele o app nao invalida a capa. */
    @Test
    void bookCardsCarryTheUpdatedAtThatBustsTheCoverCache() {
        assertThat(bookService.buscarPorGenero("Romance", PageRequest.of(0, 5)).getContent())
                .isNotEmpty()
                .allSatisfy(card -> assertThat(card.getUpdatedAt()).isNotNull());
        assertThat(bookService.buscarMobilePorTexto("a", PAGE).getContent())
                .isNotEmpty()
                .allSatisfy(card -> assertThat(card.getUpdatedAt()).isNotNull());
        // O catalogo mobile vem de query nativa, onde o tipo do timestamptz
        // depende do driver — e justamente onde a conversao podia sair nula.
        assertThat(bookService.buscarCatalogoParaMobile())
                .isNotEmpty()
                .allSatisfy(block -> assertThat(block.getBooks())
                        .allSatisfy(card -> assertThat(card.getUpdatedAt()).isNotNull()));
    }

    /** Total e disponiveis numa consulta so, batendo com o que a tabela diz. */
    @Test
    void copyCountsMatchTheCopiesTable() {
        Book book = bookWithAtLeastTwoCopies();

        BookCopyCounts counts = bookService.contarExemplares(book.getId());

        assertThat(counts.total()).isEqualTo(bookCopyRepository.countByBook_Id(book.getId()));
        assertThat(counts.available()).isEqualTo(
                bookCopyRepository.countByBookIdAndStatus(book.getId(), BookCopyStatus.AVAILABLE));
        // Agregacao sobre conjunto vazio devolve uma linha com zeros, e nao
        // nenhuma linha: livro sem exemplar responde 0/0, que e resposta.
        assertThat(bookService.contarExemplares(UUID.randomUUID())).isEqualTo(BookCopyCounts.NONE);
    }

    private Book anyBook() {
        return bookRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);
    }

    /**
     * Livro cujo acervo existe mas esta todo fora de circulacao (emprestado,
     * manutencao, indisponivel). O filtro exige {@code total > 0} de proposito:
     * desde o T07 o seed tambem tem um livro <b>sem exemplar nenhum</b>, e os
     * dois casos precisam ficar separados — e a diferenca entre "esta todo
     * emprestado" e "a biblioteca nao tem o titulo".
     */
    private Book bookWithoutAvailableCopy() {
        return bookRepository.findAll().stream()
                .filter(book -> bookCopyRepository.countByBook_Id(book.getId()) > 0)
                .filter(book -> bookCopyRepository.countByBookIdAndStatus(
                        book.getId(), BookCopyStatus.AVAILABLE) == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed should have a book with no available copy"));
    }

    /** Livro que a biblioteca simplesmente nao tem — zero exemplares cadastrados. */
    private Book bookWithNoCopyAtAll() {
        return bookRepository.findAll().stream()
                .filter(book -> bookCopyRepository.countByBook_Id(book.getId()) == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed should have a book with no copies"));
    }

    private Book bookWithAvailableCopies() {
        return bookRepository.findAll().stream()
                .filter(book -> bookCopyRepository.countByBookIdAndStatus(
                        book.getId(), BookCopyStatus.AVAILABLE) > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed should have a book with an available copy"));
    }

    private Book bookWithAtLeastTwoCopies() {
        return bookRepository.findAll().stream()
                .filter(book -> bookCopyRepository.countByBook_Id(book.getId()) >= 2)
                .findFirst()
                .orElseThrow(() -> new AssertionError("seed should have a book with two copies"));
    }

    /**
     * Principal de leitor montado a partir da linha de {@code reader}.
     *
     * <p>Monta o principal direto em vez de passar pelo login porque o que o
     * endpoint exige e um papel READER com leitor vinculado, e nao um fluxo de
     * autenticacao — o login em si e coberto pelos testes de auth. Serve tambem
     * para leitores sem conta, que sao a maioria do seed.
     */
    private void authenticateAsReader(String matricula) {
        Reader reader = readerRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> new AssertionError("seed should have reader " + matricula));
        AppUser user = new AppUser();
        user.setRole(Role.READER);
        user.setReader(reader);
        user.setActive(true);
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private void authenticateAs(String login) {
        var details = userDetailsService.loadUserByUsername(login);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
