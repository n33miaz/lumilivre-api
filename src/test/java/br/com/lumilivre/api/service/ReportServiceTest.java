package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private MessageResolver messages;

    @BeforeEach
    void setUp() {
        lenient().when(messages.resolve(anyString(), any(Locale.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Object[] rawArgs = invocation.getArguments();
                    Object[] args = rawArgs.length == 3 && rawArgs[2] instanceof Object[] nested
                            ? nested
                            : Arrays.copyOfRange(rawArgs, 2, rawArgs.length);
                    return args.length == 0 ? key : key + " " + Arrays.toString(args);
                });
    }

    @Test
    void loanReportQueriesWithNormalizedFiltersAndWritesPdf() throws Exception {
        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .borrowedAt(OffsetDateTime.parse("2026-05-01T10:00:00-03:00"))
                .status(LoanStatus.ACTIVE)
                .student(student())
                .bookCopy(copy(book()))
                .build();
        when(loanRepository.findForReport(
                any(),
                any(),
                eq(LoanStatus.ACTIVE),
                eq("%2025001%"),
                eq(1),
                eq("%T001%"),
                eq(3))).thenReturn(List.of(loan));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().gerarRelatorioEmprestimosPorFiltros(
                out,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                LoanStatus.ACTIVE,
                "2025001",
                1,
                "T001",
                3,
                Locale.US);

        assertPdf(out);
        verify(loanRepository).findForReport(
                any(),
                any(),
                eq(LoanStatus.ACTIVE),
                eq("%2025001%"),
                eq(1),
                eq("%T001%"),
                eq(3));
    }

    @Test
    void studentReportCountsLoansAcrossRelevantStatusesAndWritesPdf() throws Exception {
        Student student = student();
        when(studentRepository.findForReport(
                eq(3),
                eq(1),
                eq(2),
                eq(PenaltyCode.WARNING),
                any(),
                any())).thenReturn(List.of(student));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("2025001", LoanStatus.ACTIVE)).thenReturn(1L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("2025001", LoanStatus.COMPLETED)).thenReturn(2L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("2025001", LoanStatus.OVERDUE)).thenReturn(3L);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().gerarRelatorioAlunosPorFiltros(
                out,
                3,
                1,
                2,
                PenaltyCode.WARNING,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                Locale.US);

        assertPdf(out);
        verify(loanRepository).countByStudent_RegistrationNumberAndStatus("2025001", LoanStatus.ACTIVE);
        verify(loanRepository).countByStudent_RegistrationNumberAndStatus("2025001", LoanStatus.COMPLETED);
        verify(loanRepository).countByStudent_RegistrationNumberAndStatus("2025001", LoanStatus.OVERDUE);
    }

    @Test
    void courseReportUsesCourseStatisticsAndWritesPdf() throws Exception {
        when(courseRepository.findStatistics()).thenReturn(List.of(
                new CourseStatisticsResponse("Computer Science", 2, 5),
                new CourseStatisticsResponse("Literature", 0, 0)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().gerarRelatorioCursosGeral(out, Locale.US);

        assertPdf(out);
        verify(courseRepository).findStatistics();
    }

    @Test
    void booksReportNormalizesLikeFiltersCountsCopiesAndWritesPdf() throws Exception {
        Book book = book();
        when(bookRepository.findForReport(
                eq("%Software%"),
                eq("%Martin%"),
                eq("%Prentice%"),
                eq("005.1"),
                eq("GENERAL"),
                eq("SOFTCOVER"),
                any(),
                any())).thenReturn(List.of(book));
        when(bookCopyRepository.countByBook_Id(book.getId())).thenReturn(4L);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().gerarRelatorioLivrosFiltrados(
                out,
                "Software",
                "Martin",
                "Prentice",
                "005.1",
                "GENERAL",
                "SOFTCOVER",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                Locale.US);

        assertPdf(out);
        verify(bookCopyRepository).countByBook_Id(book.getId());
    }

    @Test
    void bookStatisticsReportUsesAuthorAndGenreCountersAndWritesPdf() throws Exception {
        when(bookRepository.count()).thenReturn(10L);
        when(bookRepository.countByAutor()).thenReturn(List.of(Map.of("autor", "Ada", "total", 3L)));
        when(bookRepository.countByGenero()).thenReturn(List.of(Map.of("genero", "Software", "total", 5L)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().gerarRelatorioEstatisticasLivros(out, Locale.US);

        assertPdf(out);
        verify(bookRepository).count();
        verify(bookRepository).countByAutor();
        verify(bookRepository).countByGenero();
    }

    @Test
    void copiesReportNormalizesCopyFilterAndWritesPdf() throws Exception {
        Book book = book();
        BookCopy copy = copy(book);
        when(bookCopyRepository.findForReport(
                eq(BookCopyStatus.AVAILABLE),
                eq("%T001%"),
                any(),
                any())).thenReturn(List.of(copy));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service().gerarRelatorioExemplaresFiltrados(
                out,
                BookCopyStatus.AVAILABLE,
                "T001",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                Locale.US);

        assertPdf(out);
        verify(bookCopyRepository).findForReport(
                eq(BookCopyStatus.AVAILABLE),
                eq("%T001%"),
                any(),
                any());
    }

    private ReportService service() {
        return new ReportService(
                loanRepository,
                studentRepository,
                bookRepository,
                courseRepository,
                bookCopyRepository,
                messages);
    }

    private static void assertPdf(ByteArrayOutputStream out) {
        assertThat(out.size()).isGreaterThan(100);
        assertThat(out.toString(StandardCharsets.ISO_8859_1)).startsWith("%PDF");
    }

    private static Student student() {
        return Student.builder()
                .registrationNumber("2025001")
                .fullName("Ada Lovelace")
                .course(new Course(1, "Computer Science", List.of()))
                .academicModule(new AcademicModule(3, "Module 1", List.of()))
                .penaltyCode(PenaltyCode.WARNING)
                .build();
    }

    private static Book book() {
        return Book.builder()
                .id(UUID.randomUUID())
                .isbn("9780132350884")
                .title("Clean Code")
                .author("Robert C. Martin")
                .genres(Set.of(new Genre(1, "Software")))
                .deweyClassification(new DeweyClassification("005.1", "Programming"))
                .build();
    }

    private static BookCopy copy(Book book) {
        return BookCopy.builder()
                .copyCode("T001")
                .status(BookCopyStatus.AVAILABLE)
                .shelfLocation("A-01")
                .book(book)
                .build();
    }
}
