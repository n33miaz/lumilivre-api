package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import br.com.lumilivre.api.dto.student.StudentListItem;
import br.com.lumilivre.api.dto.student.StudentRequest;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.service.infra.EmailService;
import br.com.lumilivre.api.service.infra.postalcode.PostalAddress;
import br.com.lumilivre.api.service.infra.postalcode.PostalCodeRouter;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StudyShiftRepository studyShiftRepository;

    @Mock
    private AcademicModuleRepository academicModuleRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PostalCodeRouter postalCodeRouter;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private MultipartFile avatarFile;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;

    @Captor
    private ArgumentCaptor<AppUser> appUserCaptor;

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void listForAdminUsesUnfilteredQueryWhenTextIsBlank() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(listItem("2025001")));
        when(studentRepository.findStudentListItems(pageable)).thenReturn(page);

        assertThat(service().listarParaAdminV2(" ", pageable)).isSameAs(page);
        verify(studentRepository, never()).findStudentListItemsByText(any(), any());
    }

    @Test
    void listForAdminUsesTextSearchWhenTextIsPresent() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(listItem("2025001")));
        when(studentRepository.findStudentListItemsByText("Ada", pageable)).thenReturn(page);

        assertThat(service().listarParaAdminV2("Ada", pageable)).isSameAs(page);
    }

    @Test
    void advancedSearchNormalizesEnumAndLikeFilters() {
        var pageable = PageRequest.of(0, 10);
        when(studentRepository.buscarAvancadoV2(
                eq(PenaltyCode.WARNING),
                eq("2025001"),
                eq("%Ada%"),
                eq("%Computer%"),
                eq(2),
                eq(3),
                eq(LocalDate.of(2001, 1, 5)),
                eq("%ada@example.test%"),
                eq("11999990000"),
                eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        service().buscarAvancadoV2(
                "warning",
                "2025001",
                "Ada",
                "Computer",
                2,
                3,
                LocalDate.of(2001, 1, 5),
                "ada@example.test",
                "11999990000",
                pageable);

        verify(studentRepository).buscarAvancadoV2(
                eq(PenaltyCode.WARNING),
                eq("2025001"),
                eq("%Ada%"),
                eq("%Computer%"),
                eq(2),
                eq(3),
                eq(LocalDate.of(2001, 1, 5)),
                eq("%ada@example.test%"),
                eq("11999990000"),
                eq(pageable));
    }

    @Test
    void createStudentCreatesLinkedAppUserAutofillsAddressAndSendsEmail() {
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);
        StudentRequest request = request()
                .postalCode("01001000")
                .penaltyCode("warning")
                .build();
        stubRelatedEntities();
        when(passwordEncoder.encode("2025001")).thenReturn("encoded-registration");
        when(postalCodeRouter.lookup("01001000", "BR")).thenReturn(Optional.of(new PostalAddress(
                "01001000", "BR", "Praca da Se", null, "Se", "Sao Paulo", "SP", "Sao Paulo")));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = service().cadastrar(request);

        assertThat(result.getRegistrationNumber()).isEqualTo("2025001");
        assertThat(result.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(result.getPenaltyCode()).isEqualTo(PenaltyCode.WARNING);
        assertThat(result.getPostalCode()).isEqualTo("01001000");
        assertThat(result.getStreet()).isEqualTo("Praca da Se");
        assertThat(result.getCity()).isEqualTo("Sao Paulo");
        assertThat(result.getStateCode()).isEqualTo("SP");
        assertThat(result.getAppUser()).isNotNull();
        assertThat(result.getAppUser().getEmail()).isEqualTo("ada@example.test");
        assertThat(result.getAppUser().getPasswordHash()).isEqualTo("encoded-registration");
        assertThat(result.getAppUser().getRole()).isEqualTo(Role.STUDENT);
        assertThat(result.getAppUser().getStudent()).isSameAs(result);
        verify(emailService).enviarSenhaInicial("ada@example.test", "Ada Lovelace", "2025001", locale);
    }

    @Test
    void createStudentRejectsDuplicateRegistrationBeforeLoadingRelations() {
        when(studentRepository.existsByRegistrationNumber("2025001")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request().build()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("student.registration.already-registered"));
        verify(courseRepository, never()).findById(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudentRejectsDuplicateEmailBeforeCreatingAppUser() {
        StudentRequest request = request().build();
        when(appUserRepository.existsByEmail("ada@example.test")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("student.email.already-in-use"));
        verify(passwordEncoder, never()).encode(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudentRefreshesCpfPasswordAndLinkedUserEmail() {
        Student existing = existingStudent();
        existing.setCpf("11111111111");
        existing.setEmail("old@example.test");
        AppUser appUser = AppUser.builder()
                .email("old@example.test")
                .passwordHash("old-hash")
                .role(Role.STUDENT)
                .student(existing)
                .build();
        existing.setAppUser(appUser);
        StudentRequest request = request()
                .cpf("22222222222")
                .email("new@example.test")
                .postalCode("bad")
                .build();
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existing));
        when(studentRepository.existsByCpf("22222222222")).thenReturn(false);
        stubRelatedEntities();
        when(passwordEncoder.encode("22222222222")).thenReturn("new-cpf-hash");
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = service().atualizar("2025001", request);

        assertThat(result.getCpf()).isEqualTo("22222222222");
        assertThat(result.getEmail()).isEqualTo("new@example.test");
        assertThat(result.getAppUser().getEmail()).isEqualTo("new@example.test");
        assertThat(result.getAppUser().getPasswordHash()).isEqualTo("new-cpf-hash");
        verify(postalCodeRouter, never()).lookup(any(), any());
    }

    @Test
    void updateStudentRejectsDuplicateChangedCpf() {
        Student existing = existingStudent();
        existing.setCpf("11111111111");
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existing));
        when(studentRepository.existsByCpf("22222222222")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().atualizar("2025001",
                        request().cpf("22222222222").build()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("student.cpf.already-in-use-by-other"));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void deleteStudentRemovesLinkedAppUserBeforeStudent() {
        Student student = existingStudent();
        AppUser appUser = AppUser.builder().email("ada@example.test").role(Role.STUDENT).build();
        student.setAppUser(appUser);
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(student));

        service().excluir("2025001");

        verify(appUserRepository).delete(appUser);
        verify(studentRepository).delete(student);
    }

    @Test
    void resetPasswordUsesRegistrationNumberForLinkedAppUser() {
        Student student = existingStudent();
        AppUser appUser = AppUser.builder()
                .email("ada@example.test")
                .passwordHash("old")
                .role(Role.STUDENT)
                .build();
        student.setAppUser(appUser);
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(student));
        when(passwordEncoder.encode("2025001")).thenReturn("encoded-registration");

        service().resetarSenha("2025001");

        verify(appUserRepository).save(appUserCaptor.capture());
        assertThat(appUserCaptor.getValue().getPasswordHash()).isEqualTo("encoded-registration");
    }

    @Test
    void resetPasswordRejectsStudentWithoutLinkedAppUser() {
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existingStudent()));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().resetarSenha("2025001"))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("student.no-app-user-linked"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void uploadAvatarStoresReturnedUrl() {
        Student student = existingStudent();
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(student));
        when(storageProvider.upload(avatarFile, StorageBucket.AVATARS)).thenReturn("https://cdn.test/avatar.jpg");

        service().uploadFoto("2025001", avatarFile);

        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getAvatarUrl()).isEqualTo("https://cdn.test/avatar.jpg");
    }

    @Test
    void uploadAvatarWrapsStorageFailure() {
        when(studentRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existingStudent()));
        when(storageProvider.upload(avatarFile, StorageBucket.AVATARS)).thenThrow(new RuntimeException("storage down"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().uploadFoto("2025001", avatarFile))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("student.avatar.upload-failed"));
        verify(studentRepository, never()).save(any());
    }

    private StudentService service() {
        return new StudentService(
                studentRepository,
                courseRepository,
                appUserRepository,
                studyShiftRepository,
                academicModuleRepository,
                emailService,
                passwordEncoder,
                postalCodeRouter,
                storageProvider);
    }

    private void stubRelatedEntities() {
        when(courseRepository.findById(1)).thenReturn(Optional.of(course()));
        when(studyShiftRepository.findById(2)).thenReturn(Optional.of(studyShift()));
        when(academicModuleRepository.findById(3)).thenReturn(Optional.of(academicModule()));
    }

    private static StudentRequest.StudentRequestBuilder request() {
        return StudentRequest.builder()
                .registrationNumber("2025001")
                .fullName("Ada Lovelace")
                .cpf("11111111111")
                .birthDate(LocalDate.of(2001, 1, 5))
                .phoneNumber("11999990000")
                .email("ada@example.test")
                .courseId(1)
                .studyShiftId(2)
                .academicModuleId(3)
                .streetNumber(10)
                .addressComplement("Room 1");
    }

    private static Student existingStudent() {
        return Student.builder()
                .registrationNumber("2025001")
                .fullName("Ada Lovelace")
                .course(course())
                .studyShift(studyShift())
                .academicModule(academicModule())
                .build();
    }

    private static Course course() {
        return new Course(1, "Computer Science", List.of());
    }

    private static StudyShift studyShift() {
        return new StudyShift(2, "Morning", List.of());
    }

    private static AcademicModule academicModule() {
        return new AcademicModule(3, "Module 1", List.of());
    }

    private static StudentListItem listItem(String registrationNumber) {
        return new StudentListItem(
                null,
                registrationNumber,
                "Computer Science",
                "Ada Lovelace",
                LocalDate.of(2001, 1, 5),
                "ada@example.test",
                "11999990000");
    }
}
