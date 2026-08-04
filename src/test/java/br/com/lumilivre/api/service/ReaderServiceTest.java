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

import br.com.lumilivre.api.dto.reader.ReaderListItem;
import br.com.lumilivre.api.dto.reader.ReaderRequest;
import br.com.lumilivre.api.enums.LibraryType;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.ReaderRepository;
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
class ReaderServiceTest {

    @Mock
    private ReaderRepository readerRepository;

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
    private SettingsService settingsService;

    @Mock
    private MultipartFile avatarFile;

    @Captor
    private ArgumentCaptor<Reader> readerCaptor;

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
        when(readerRepository.findReaderListItems(pageable)).thenReturn(page);

        assertThat(service().listarParaAdminV2(" ", pageable)).isSameAs(page);
        verify(readerRepository, never()).findReaderListItemsByText(any(), any());
    }

    @Test
    void listForAdminUsesTextSearchWhenTextIsPresent() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(listItem("2025001")));
        when(readerRepository.findReaderListItemsByText("Ada", pageable)).thenReturn(page);

        assertThat(service().listarParaAdminV2("Ada", pageable)).isSameAs(page);
    }

    @Test
    void advancedSearchNormalizesEnumAndLikeFilters() {
        var pageable = PageRequest.of(0, 10);
        when(readerRepository.buscarAvancadoV2(
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

        verify(readerRepository).buscarAvancadoV2(
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
    void createReaderCreatesLinkedAppUserAutofillsAddressAndSendsEmail() {
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);
        ReaderRequest request = request()
                .postalCode("01001000")
                .penaltyCode("warning")
                .build();
        stubRelatedEntities();
        when(passwordEncoder.encode("2025001")).thenReturn("encoded-registration");
        when(postalCodeRouter.lookup("01001000", "BR")).thenReturn(Optional.of(new PostalAddress(
                "01001000", "BR", "Praca da Se", null, "Se", "Sao Paulo", "SP", "Sao Paulo")));
        when(readerRepository.save(any(Reader.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reader result = service().cadastrar(request);

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
        assertThat(result.getAppUser().getRole()).isEqualTo(Role.READER);
        assertThat(result.getAppUser().getReader()).isSameAs(result);
        verify(emailService).enviarSenhaInicial("ada@example.test", "Ada Lovelace", "2025001", locale);
    }

    @Test
    void createReaderRejectsDuplicateRegistrationBeforeLoadingRelations() {
        when(readerRepository.existsByRegistrationNumber("2025001")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request().build()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("reader.registration.already-registered"));
        verify(courseRepository, never()).findById(any());
        verify(readerRepository, never()).save(any());
    }

    @Test
    void createReaderRejectsDuplicateEmailBeforeCreatingAppUser() {
        ReaderRequest request = request().build();
        when(appUserRepository.existsByEmail("ada@example.test")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("reader.email.already-in-use"));
        verify(passwordEncoder, never()).encode(any());
        verify(readerRepository, never()).save(any());
    }

    @Test
    void updateReaderKeepsPasswordOnCpfChange() {
        Reader existing = existingReader();
        existing.setCpf("11111111111");
        existing.setEmail("old@example.test");
        AppUser appUser = AppUser.builder()
                .email("old@example.test")
                .passwordHash("old-hash")
                .role(Role.READER)
                .reader(existing)
                .build();
        existing.setAppUser(appUser);
        ReaderRequest request = request()
                .cpf("22222222222")
                .email("new@example.test")
                .postalCode("bad")
                .build();
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existing));
        when(readerRepository.existsByCpf("22222222222")).thenReturn(false);
        stubRelatedEntities();
        when(readerRepository.save(any(Reader.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reader result = service().atualizar("2025001", request);

        assertThat(result.getCpf()).isEqualTo("22222222222");
        assertThat(result.getEmail()).isEqualTo("new@example.test");
        assertThat(result.getAppUser().getEmail()).isEqualTo("new@example.test");
        // SEC-03: editar o CPF não pode mais resetar a senha para um valor previsível.
        assertThat(result.getAppUser().getPasswordHash()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(any());
        verify(postalCodeRouter, never()).lookup(any(), any());
    }

    @Test
    void updateReaderRejectsDuplicateChangedCpf() {
        Reader existing = existingReader();
        existing.setCpf("11111111111");
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existing));
        when(readerRepository.existsByCpf("22222222222")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().atualizar("2025001",
                        request().cpf("22222222222").build()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("reader.cpf.already-in-use-by-other"));
        verify(readerRepository, never()).save(any());
    }

    @Test
    void deleteReaderRemovesLinkedAppUserBeforeReader() {
        Reader reader = existingReader();
        AppUser appUser = AppUser.builder().email("ada@example.test").role(Role.READER).build();
        reader.setAppUser(appUser);
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(reader));

        service().excluir("2025001");

        verify(appUserRepository).delete(appUser);
        verify(readerRepository).delete(reader);
    }

    @Test
    void resetPasswordUsesRegistrationNumberForLinkedAppUser() {
        Reader reader = existingReader();
        AppUser appUser = AppUser.builder()
                .email("ada@example.test")
                .passwordHash("old")
                .role(Role.READER)
                .build();
        reader.setAppUser(appUser);
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(reader));
        when(passwordEncoder.encode("2025001")).thenReturn("encoded-registration");

        service().resetarSenha("2025001");

        verify(appUserRepository).save(appUserCaptor.capture());
        assertThat(appUserCaptor.getValue().getPasswordHash()).isEqualTo("encoded-registration");
    }

    @Test
    void resetPasswordRejectsReaderWithoutLinkedAppUser() {
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existingReader()));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().resetarSenha("2025001"))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("reader.no-app-user-linked"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void uploadAvatarStoresReturnedUrl() {
        Reader reader = existingReader();
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(reader));
        when(storageProvider.upload(avatarFile, StorageBucket.AVATARS)).thenReturn("https://cdn.test/avatar.jpg");

        service().uploadFoto("2025001", avatarFile);

        verify(readerRepository).save(readerCaptor.capture());
        assertThat(readerCaptor.getValue().getAvatarUrl()).isEqualTo("https://cdn.test/avatar.jpg");
    }

    @Test
    void uploadAvatarWrapsStorageFailure() {
        when(readerRepository.findByRegistrationNumber("2025001")).thenReturn(Optional.of(existingReader()));
        when(storageProvider.upload(avatarFile, StorageBucket.AVATARS)).thenThrow(new RuntimeException("storage down"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().uploadFoto("2025001", avatarFile))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("reader.avatar.upload-failed"));
        verify(readerRepository, never()).save(any());
    }

    private ReaderService service() {
        return new ReaderService(
                readerRepository,
                courseRepository,
                appUserRepository,
                studyShiftRepository,
                academicModuleRepository,
                emailService,
                passwordEncoder,
                postalCodeRouter,
                storageProvider,
                settingsService);
    }

    private void stubRelatedEntities() {
        when(settingsService.getLibraryType()).thenReturn(LibraryType.SCHOOL);
        when(courseRepository.findById(1)).thenReturn(Optional.of(course()));
        when(studyShiftRepository.findById(2)).thenReturn(Optional.of(studyShift()));
        when(academicModuleRepository.findById(3)).thenReturn(Optional.of(academicModule()));
    }

    private static ReaderRequest.ReaderRequestBuilder request() {
        return ReaderRequest.builder()
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

    private static Reader existingReader() {
        return Reader.builder()
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

    private static ReaderListItem listItem(String registrationNumber) {
        return new ReaderListItem(
                null,
                registrationNumber,
                "Computer Science",
                null,
                "Ada Lovelace",
                LocalDate.of(2001, 1, 5),
                "ada@example.test",
                "11999990000");
    }
}
