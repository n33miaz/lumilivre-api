package br.com.lumilivre.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.DeweyClassification;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.StudentRepository;

/**
 * Manual preview harness (NOT part of the CI suite — its name does not match the
 * Surefire include patterns). Renders real, fully-internationalized PDFs for the
 * main report types into {@code target/report-previews/}. Run on demand:
 *
 * <pre>./mvnw -Dtest=ReportPreviewGenerator test</pre>
 */
class ReportPreviewGenerator {

    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final Locale EN = Locale.forLanguageTag("en-US");

    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final BookCopyRepository bookCopyRepository = mock(BookCopyRepository.class);

    @Test
    void generate() throws Exception {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:i18n/report/messages", "classpath:i18n/enum/messages",
                "classpath:i18n/common/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        MessageResolver messages = new MessageResolver(source);

        stubData();
        ReportService service = new ReportService(loanRepository, studentRepository, bookRepository,
                courseRepository, bookCopyRepository, messages);

        Path dir = Path.of("target", "report-previews");
        Files.createDirectories(dir);

        try (OutputStream o = Files.newOutputStream(dir.resolve("01-loans-pt.pdf"))) {
            service.gerarRelatorioEmprestimosPorFiltros(o, null, null, null, null, null, null, null, PT);
        }
        try (OutputStream o = Files.newOutputStream(dir.resolve("02-loans-en.pdf"))) {
            service.gerarRelatorioEmprestimosPorFiltros(o, null, null, null, null, null, null, null, EN);
        }
        try (OutputStream o = Files.newOutputStream(dir.resolve("03-students-pt.pdf"))) {
            service.gerarRelatorioAlunosPorFiltros(o, null, null, null, null, null, null, PT);
        }
        try (OutputStream o = Files.newOutputStream(dir.resolve("04-copies-pt.pdf"))) {
            service.gerarRelatorioExemplaresFiltrados(o, null, null, null, null, PT);
        }
        try (OutputStream o = Files.newOutputStream(dir.resolve("05-books-statistics-en.pdf"))) {
            service.gerarRelatorioEstatisticasLivros(o, EN);
        }
        try (OutputStream o = Files.newOutputStream(dir.resolve("06-courses-pt.pdf"))) {
            service.gerarRelatorioCursosGeral(o, PT);
        }
        try (OutputStream o = Files.newOutputStream(dir.resolve("07-books-pt.pdf"))) {
            service.gerarRelatorioLivrosFiltrados(o, null, null, null, null, null, null, null, null, PT);
        }

        System.out.println("[report-previews] written to: " + dir.toAbsolutePath());
    }

    private void stubData() {
        lenient().when(loanRepository.findForReport(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleLoans());
        lenient().when(loanRepository.countByStudent_RegistrationNumberAndStatus(anyString(), any()))
                .thenReturn(2L);
        lenient().when(studentRepository.findForReport(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleStudents());
        lenient().when(bookRepository.findForReport(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleBooks());
        lenient().when(bookCopyRepository.countByBook_Id(any())).thenReturn(3L);
        lenient().when(bookCopyRepository.findForReport(any(), any(), any(), any()))
                .thenReturn(sampleCopies());
        lenient().when(bookRepository.count()).thenReturn(128L);
        lenient().when(bookRepository.countByAutor()).thenReturn(List.of(
                Map.of("autor", "Machado de Assis", "total", 9L),
                Map.of("autor", "Clarice Lispector", "total", 7L),
                Map.of("autor", "J. R. R. Tolkien", "total", 6L),
                Map.of("autor", "Robert C. Martin", "total", 4L),
                Map.of("autor", "Ada Lovelace", "total", 3L)));
        lenient().when(bookRepository.countByGenero()).thenReturn(List.of(
                Map.of("genero", "Romance", "total", 34L),
                Map.of("genero", "Tecnologia", "total", 22L),
                Map.of("genero", "Fantasia", "total", 18L),
                Map.of("genero", "História", "total", 11L)));
        lenient().when(courseRepository.findStatistics()).thenReturn(List.of(
                new CourseStatisticsResponse("Análise e Desenvolvimento de Sistemas", 42, 118),
                new CourseStatisticsResponse("Letras", 28, 64),
                new CourseStatisticsResponse("Pedagogia", 35, 51),
                new CourseStatisticsResponse("Administração", 19, 27)));
    }

    private List<Loan> sampleLoans() {
        return List.of(
                loan("3a1f", LoanStatus.ACTIVE, student("2025001", "Ada Lovelace", "Análise e Desenvolvimento de Sistemas", null),
                        copy("T-0012", "Clean Code"), 4),
                loan("7c2e", LoanStatus.OVERDUE, student("2025002", "Alan Turing", "Análise e Desenvolvimento de Sistemas", PenaltyCode.SUSPENSION),
                        copy("T-0048", "O Senhor dos Anéis"), 20),
                loan("9b4d", LoanStatus.COMPLETED, student("2025003", "Grace Hopper", "Letras", null),
                        copy("T-0103", "Dom Casmurro"), 9),
                loan("1e8a", LoanStatus.ACTIVE, student("2025004", "Carlos Drummond", "Letras", null),
                        copy("T-0210", "A Hora da Estrela"), 2),
                loan("5d6c", LoanStatus.COMPLETED, student("2025005", "Marie Curie", "Pedagogia", PenaltyCode.WARNING),
                        copy("T-0077", "Vidas Secas"), 14),
                loan("2f9b", LoanStatus.OVERDUE, student("2025006", "Nikola Tesla", "Administração", null),
                        copy("T-0301", "O Cortiço"), 31));
    }

    private List<Student> sampleStudents() {
        return List.of(
                student("2025001", "Ada Lovelace", "Análise e Desenvolvimento de Sistemas", null),
                student("2025002", "Alan Turing", "Análise e Desenvolvimento de Sistemas", PenaltyCode.SUSPENSION),
                student("2025003", "Grace Hopper", "Letras", null),
                student("2025004", "Carlos Drummond", "Letras", PenaltyCode.WARNING),
                student("2025005", "Marie Curie", "Pedagogia", null),
                student("2025006", "Nikola Tesla", "Administração", PenaltyCode.BAN));
    }

    private List<Book> sampleBooks() {
        return List.of(
                book("Clean Code", "Robert C. Martin", "Tecnologia", "005.1"),
                book("O Senhor dos Anéis", "J. R. R. Tolkien", "Fantasia", "823.9"),
                book("Dom Casmurro", "Machado de Assis", "Romance", "869.3"),
                book("A Hora da Estrela", "Clarice Lispector", "Romance", "869.3"),
                book("Vidas Secas", "Graciliano Ramos", "Romance", "869.3"),
                book("O Cortiço", "Aluísio Azevedo", "Romance", "869.3"));
    }

    private List<BookCopy> sampleCopies() {
        return List.of(
                copyFull("T-0012", "Clean Code", BookCopyStatus.AVAILABLE, "A-01", "9780132350884"),
                copyFull("T-0048", "O Senhor dos Anéis", BookCopyStatus.BORROWED, "B-14", "9788533613379"),
                copyFull("T-0103", "Dom Casmurro", BookCopyStatus.AVAILABLE, "C-07", "9788535910663"),
                copyFull("T-0210", "A Hora da Estrela", BookCopyStatus.MAINTENANCE, "C-09", "9788532508126"),
                copyFull("T-0077", "Vidas Secas", BookCopyStatus.UNAVAILABLE, "D-02", "9788503012261"),
                copyFull("T-0301", "O Cortiço", BookCopyStatus.BORROWED, "D-05", "9788508040962"));
    }

    private Loan loan(String idPrefix, LoanStatus status, Student student, BookCopy copy, int daysAgo) {
        return Loan.builder()
                .id(UUID.nameUUIDFromBytes(("loan-" + idPrefix).getBytes()))
                .status(status)
                .student(student)
                .bookCopy(copy)
                .borrowedAt(OffsetDateTime.now().minusDays(daysAgo))
                .dueAt(OffsetDateTime.now().minusDays(daysAgo).plusDays(14))
                .build();
    }

    private Student student(String reg, String name, String course, PenaltyCode penalty) {
        return Student.builder()
                .registrationNumber(reg)
                .fullName(name)
                .course(new Course(1, course, List.of()))
                .academicModule(new AcademicModule(2, "M2", List.of()))
                .penaltyCode(penalty)
                .build();
    }

    private BookCopy copy(String code, String title) {
        return BookCopy.builder().copyCode(code).status(BookCopyStatus.BORROWED)
                .book(Book.builder().id(UUID.randomUUID()).title(title).build()).build();
    }

    private BookCopy copyFull(String code, String title, BookCopyStatus status, String shelf, String isbn) {
        return BookCopy.builder().copyCode(code).status(status).shelfLocation(shelf)
                .book(Book.builder().id(UUID.randomUUID()).title(title).isbn(isbn).build()).build();
    }

    private Book book(String title, String author, String genre, String dewey) {
        return Book.builder()
                .id(UUID.randomUUID())
                .title(title)
                .author(author)
                .genres(Set.of(new Genre(1, genre)))
                .deweyClassification(new DeweyClassification(dewey, genre))
                .build();
    }
}
