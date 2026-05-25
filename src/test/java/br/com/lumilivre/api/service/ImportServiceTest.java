package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.DeweyClassification;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudyShiftRepository studyShiftRepository;

    @Mock
    private AcademicModuleRepository academicModuleRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private DeweyClassificationRepository deweyClassificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageResolver messages;

    @Captor
    private ArgumentCaptor<List<Student>> studentsCaptor;

    @Captor
    private ArgumentCaptor<List<Book>> booksCaptor;

    @Captor
    private ArgumentCaptor<List<BookCopy>> copiesCaptor;

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
    void importRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service().importar("livro", file, Locale.US))
                .withMessage("import.error.file.empty");
    }

    @Test
    void importRejectsInvalidContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "books.csv", "text/csv", "isbn".getBytes());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service().importar("livro", file, Locale.US))
                .withMessage("import.error.file.invalid-format");
    }

    @Test
    void importBooksParsesRowsAndSavesBatch() throws Exception {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        DeweyClassification dewey = new DeweyClassification("005.1", "Programming");
        when(deweyClassificationRepository.findById("005.1")).thenReturn(Optional.of(dewey));
        when(bookRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String summary = service().importar("livro", xlsx("books", sheet -> {
            header(sheet, "isbn", "nome", "autor", "editora", "data_lancamento", "numero_paginas",
                    "edicao", "volume", "sinopse", "imagem", "classificacao_etaria", "tipo_capa",
                    "cdd_codigo");
            row(sheet, 1, "9780132350884", "Clean Code", "Robert C. Martin", "Prentice Hall",
                    "01/08/2008", 464, "1st", 1, "Craftsmanship", "https://cdn.test/cover.jpg",
                    "general", "softcover", "005.1");
        }), Locale.US);

        verify(bookRepository).saveAll(booksCaptor.capture());
        assertThat(booksCaptor.getValue()).hasSize(1);
        Book book = booksCaptor.getValue().get(0);
        assertThat(book.getIsbn()).isEqualTo("9780132350884");
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(book.getAgeRating()).isEqualTo(AgeRating.GENERAL);
        assertThat(book.getCoverType()).isEqualTo(CoverType.SOFTCOVER);
        assertThat(book.getDeweyClassification()).isSameAs(dewey);
        assertThat(book.getGenres()).isEmpty();
        assertThat(summary).contains("email.import.summary");
    }

    @Test
    void importBooksReportsDuplicateIsbnAndSavesOnlyUniqueRows() throws Exception {
        when(bookRepository.existsByIsbn("9780132350884")).thenReturn(false);
        when(deweyClassificationRepository.findById("005.1"))
                .thenReturn(Optional.of(new DeweyClassification("005.1", "Programming")));
        when(bookRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String summary = service().importar("livro", xlsx("books", sheet -> {
            header(sheet, "isbn", "nome", "autor", "editora", "data_lancamento", "numero_paginas",
                    "edicao", "volume", "sinopse", "imagem", "classificacao_etaria", "tipo_capa",
                    "cdd_codigo");
            row(sheet, 1, "9780132350884", "Clean Code", "Robert C. Martin", "Prentice Hall",
                    "01/08/2008", 464, "1st", 1, "Craftsmanship", "cover-1",
                    "general", "softcover", "005.1");
            row(sheet, 2, "9780132350884", "Duplicate", "Someone", "Publisher",
                    "01/01/2020", 120, "1st", 1, "Duplicate", "cover-2",
                    "general", "softcover", "005.1");
        }), Locale.US);

        verify(bookRepository).saveAll(booksCaptor.capture());
        assertThat(booksCaptor.getValue()).hasSize(1);
        assertThat(summary).contains("import.error.isbn.duplicate-in-sheet");
    }

    @Test
    void importCopiesLinksRowsToBooks() throws Exception {
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder().id(bookId).title("Clean Code").build();
        when(bookCopyRepository.existsByCopyCode("T001")).thenReturn(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookCopyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().importar("exemplar", xlsx("copies", sheet -> {
            header(sheet, "tombo", "livro_id", "localizacao_fisica", "status_livro");
            row(sheet, 1, "T001", bookId.toString(), "A-01", "borrowed");
        }), Locale.US);

        verify(bookCopyRepository).saveAll(copiesCaptor.capture());
        BookCopy copy = copiesCaptor.getValue().get(0);
        assertThat(copy.getCopyCode()).isEqualTo("T001");
        assertThat(copy.getBook()).isSameAs(book);
        assertThat(copy.getShelfLocation()).isEqualTo("A-01");
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.BORROWED);
    }

    @Test
    void importStudentsCreatesStudentUsersAndNormalizesNumbers() throws Exception {
        when(studentRepository.findAllMatriculas()).thenReturn(Set.of());
        when(studentRepository.findAllCpfs()).thenReturn(Set.of());
        when(courseRepository.findAll()).thenReturn(List.of(new Course(1, "Computer Science", List.of())));
        when(studyShiftRepository.findAll()).thenReturn(List.of(new StudyShift(2, "Morning", List.of())));
        when(academicModuleRepository.findAll()).thenReturn(List.of(new AcademicModule(3, "Module 1", List.of())));
        when(passwordEncoder.encode("2025001")).thenReturn("encoded-registration");
        when(studentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().importar("aluno", xlsx("students", sheet -> {
            header(sheet, "matricula", "nome_completo", "cpf", "celular", "email", "data_nascimento",
                    "cep", "logradouro", "bairro", "localidade", "uf", "numero_casa", "complemento",
                    "curso_id", "turno_id", "modulo_id");
            row(sheet, 1, "2025001", "Ada Lovelace", "111.111.111-11", "(11) 99999-0000",
                    "ada@example.test", "05/01/2001", "01001-000", "Praca da Se", "Se",
                    "Sao Paulo", "SP", 10, "Room 1", 1, 2, 3);
        }), Locale.US);

        verify(studentRepository).saveAll(studentsCaptor.capture());
        Student student = studentsCaptor.getValue().get(0);
        assertThat(student.getRegistrationNumber()).isEqualTo("2025001");
        assertThat(student.getCpf()).isEqualTo("11111111111");
        assertThat(student.getPhoneNumber()).isEqualTo("11999990000");
        assertThat(student.getCourse().getId()).isEqualTo(1);
        assertThat(student.getStudyShift().getId()).isEqualTo(2);
        assertThat(student.getAcademicModule().getId()).isEqualTo(3);
        assertThat(student.getAppUser()).isNotNull();
        assertThat(student.getAppUser().getRole()).isEqualTo(Role.STUDENT);
        assertThat(student.getAppUser().getPasswordHash()).isEqualTo("encoded-registration");
        verify(appUserRepository).existsByEmail("ada@example.test");
    }

    @Test
    void importStudentsReportsInvalidCourseAndDoesNotSave() throws Exception {
        when(studentRepository.findAllMatriculas()).thenReturn(Set.of());
        when(studentRepository.findAllCpfs()).thenReturn(Set.of());
        when(courseRepository.findAll()).thenReturn(List.of());
        when(studyShiftRepository.findAll()).thenReturn(List.of(new StudyShift(2, "Morning", List.of())));
        when(academicModuleRepository.findAll()).thenReturn(List.of(new AcademicModule(3, "Module 1", List.of())));

        String summary = service().importar("aluno", xlsx("students", sheet -> {
            header(sheet, "matricula", "nome_completo", "cpf", "celular", "email", "data_nascimento",
                    "cep", "logradouro", "bairro", "localidade", "uf", "numero_casa", "complemento",
                    "curso_id", "turno_id", "modulo_id");
            row(sheet, 1, "2025001", "Ada Lovelace", "11111111111", "11999990000",
                    "ada@example.test", "05/01/2001", "01001000", "Praca da Se", "Se",
                    "Sao Paulo", "SP", 10, "Room 1", 999, 2, 3);
        }), Locale.US);

        verify(studentRepository, never()).saveAll(any());
        assertThat(summary).contains("import.error.course.invalid");
    }

    private ImportService service() {
        return new ImportService(
                studentRepository,
                appUserRepository,
                courseRepository,
                studyShiftRepository,
                academicModuleRepository,
                bookRepository,
                bookCopyRepository,
                deweyClassificationRepository,
                passwordEncoder,
                messages);
    }

    private static MockMultipartFile xlsx(String name, Consumer<Sheet> writer) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            writer.accept(sheet);
            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    name + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private static void header(Sheet sheet, String... values) {
        row(sheet, 0, (Object[]) values);
    }

    private static void row(Sheet sheet, int rowIndex, Object... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Number number) {
                row.createCell(i).setCellValue(number.doubleValue());
            } else {
                row.createCell(i).setCellValue(values[i] == null ? "" : values[i].toString());
            }
        }
    }
}
