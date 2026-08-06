package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.dto.content.ContentRequest;
import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppContent;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppContentRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.security.CustomUserDetails;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;

/**
 * A regra de audiência do mural, que é regra de segurança e não de layout: o
 * aluno só alcança o que o feed lhe entregaria. Um rascunho aberto pela URL
 * direta é um comunicado vazando antes da hora; um conteúdo de outro curso é
 * segmentação furada.
 *
 * <p>O que este teste <b>não</b> cobre de propósito: a consulta do feed em si
 * (a janela e a segmentação moram no JPQL do repositório) — isso vive em
 * {@code ContentFeedPostgresTest}, contra Postgres de verdade.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppContentServiceTest {

    private static final UUID CONTENT_ID = UUID.fromString("00000000-0000-4000-8000-000000009001");
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Mock private AppContentRepository contentRepository;
    @Mock private StorageProvider storageProvider;
    @Mock private CourseRepository courseRepository;
    @Mock private AcademicModuleRepository academicModuleRepository;
    @Mock private StudyShiftRepository studyShiftRepository;

    private AppContentService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AppContentService(contentRepository, storageProvider,
                courseRepository, academicModuleRepository, studyShiftRepository);
        when(contentRepository.save(any(AppContent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- audiência: quem alcança o conteúdo pela URL direta -------------------

    @Test
    void aReaderDoesNotOpenADraftByGuessingTheUrl() {
        AppContent draft = published(AudienceScope.ALL);
        draft.setPublished(false);
        stored(draft);
        authenticateReader(null, null, null);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));
    }

    @Test
    void aReaderDoesNotOpenAScheduledContentBeforeItsWindowOpens() {
        AppContent scheduled = published(AudienceScope.ALL);
        scheduled.setPublishStartAt(NOW.plusDays(2));
        stored(scheduled);
        authenticateReader(null, null, null);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));
    }

    @Test
    void aReaderDoesNotOpenAContentWhoseWindowAlreadyClosed() {
        AppContent expired = published(AudienceScope.ALL);
        expired.setPublishEndAt(NOW.minusMinutes(1));
        stored(expired);
        authenticateReader(null, null, null);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));
    }

    /**
     * Recusa é 404 e não 403 de propósito: um 403 confirmaria que existe um
     * comunicado naquele id — e a existência já é informação quando o conteúdo é
     * segmentado por turma.
     */
    @Test
    void aReaderDoesNotOpenAContentAddressedToAnotherCourse() {
        AppContent restricted = published(AudienceScope.COURSE);
        restricted.setCourse(course(7));
        stored(restricted);
        authenticateReader(9, null, null);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));
    }

    @Test
    void aReaderOpensTheContentOfTheirOwnCourse() {
        AppContent restricted = published(AudienceScope.COURSE);
        restricted.setCourse(course(7));
        stored(restricted);
        authenticateReader(7, null, null);

        assertThat(service.getById(CONTENT_ID).getTitle()).isEqualTo("Comunicado");
    }

    @Test
    void moduleAndShiftSegmentEachOnItsOwnField() {
        AppContent byModule = published(AudienceScope.MODULE);
        byModule.setAcademicModule(academicModule(3));
        stored(byModule);

        // Mesmo número, campo errado: o leitor está no curso 3, não no módulo 3.
        authenticateReader(3, null, null);
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));

        authenticateReader(null, 3, null);
        assertThat(service.getById(CONTENT_ID)).isNotNull();

        AppContent byShift = published(AudienceScope.SHIFT);
        byShift.setStudyShift(studyShift(2));
        stored(byShift);

        authenticateReader(null, 3, null);
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));

        authenticateReader(null, null, 2);
        assertThat(service.getById(CONTENT_ID)).isNotNull();
    }

    /**
     * Conta de leitor sem vínculo acadêmico existe (cadastro incompleto, aluno
     * transferido). Ela enxerga o mural geral e nada mais — nunca o oposto, que
     * seria "sem vínculo, vê tudo".
     */
    @Test
    void aReaderWithoutAnyAcademicLinkSeesOnlyTheGeneralNotices() {
        stored(published(AudienceScope.ALL));
        authenticateReader(null, null, null);
        assertThat(service.getById(CONTENT_ID)).isNotNull();

        AppContent restricted = published(AudienceScope.COURSE);
        restricted.setCourse(course(7));
        stored(restricted);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));
    }

    /** O painel enxerga o próprio rascunho: senão não haveria como revisá-lo. */
    @Test
    void staffOpensWhatTheReaderCannot() {
        AppContent draft = published(AudienceScope.COURSE);
        draft.setPublished(false);
        draft.setPublishStartAt(NOW.plusDays(5));
        draft.setCourse(course(7));
        stored(draft);
        authenticateStaff(Role.LIBRARIAN);

        assertThat(service.getById(CONTENT_ID).getTitle()).isEqualTo("Comunicado");
    }

    @Test
    void aRemovedContentIsGoneForEveryone() {
        AppContent removed = published(AudienceScope.ALL);
        removed.setDeletedAt(NOW.minusDays(1));
        stored(removed);
        authenticateStaff(Role.ADMIN);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.getById(CONTENT_ID));
    }

    // ---- feed: a segmentação vem do token, não do pedido ---------------------

    /**
     * Os três ids de segmentação saem do principal autenticado. Se viessem do
     * cliente, qualquer leitor pediria o mural de outra turma trocando um
     * parâmetro.
     */
    @Test
    void theFeedTakesTheSegmentationFromThePrincipal() {
        authenticateReader(7, 3, 2);
        when(contentRepository.findFeed(eq(7), eq(3), eq(2), any())).thenReturn(List.of(published(AudienceScope.ALL)));

        assertThat(service.feedForCurrentReader()).hasSize(1);
        verify(contentRepository).findFeed(eq(7), eq(3), eq(2), any(OffsetDateTime.class));
    }

    @Test
    void theFeedOfAStaffAccountCarriesNoSegmentation() {
        authenticateStaff(Role.ADMIN);
        when(contentRepository.findFeed(any(), any(), any(), any())).thenReturn(List.of());

        service.feedForCurrentReader();

        verify(contentRepository).findFeed(eq(null), eq(null), eq(null), any(OffsetDateTime.class));
    }

    // ---- escrita -------------------------------------------------------------

    @Test
    void contentWithoutTypeOrTitleIsRefusedBeforeTouchingStorage() {
        ContentRequest noType = ContentRequest.builder().title("Comunicado").build();
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(noType, null, null));

        ContentRequest blankTitle = ContentRequest.builder()
                .contentType(ContentType.ANNOUNCEMENT).title("   ").build();
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(blankTitle, null, null));

        verify(contentRepository, never()).save(any());
    }

    @Test
    void aCourseScopedContentDemandsACourse() {
        ContentRequest request = base().audienceScope(AudienceScope.COURSE).build();

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(request, null, null))
                .withMessageContaining("content.audience.course-required");
    }

    @Test
    void aCourseThatDoesNotExistIsNotFoundAndNotAnEmptyAudience() {
        ContentRequest request = base().audienceScope(AudienceScope.COURSE).courseId(404).build();
        when(courseRepository.findById(404)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.create(request, null, null));
    }

    @Test
    void moduleAndShiftScopesDemandTheirOwnTarget() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(base().audienceScope(AudienceScope.MODULE).build(), null, null))
                .withMessageContaining("content.audience.module-required");

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(base().audienceScope(AudienceScope.SHIFT).build(), null, null))
                .withMessageContaining("content.audience.shift-required");
    }

    /**
     * Trocar o escopo tem que <b>apagar</b> o alvo anterior. Um conteúdo que
     * volta a ser geral carregando o curso antigo continuaria filtrado pelo
     * feed: publicado para todos na tela do painel, invisível para metade da
     * escola no celular.
     */
    @Test
    void wideningTheAudienceClearsTheOldTarget() {
        AppContent existing = published(AudienceScope.COURSE);
        existing.setCourse(course(7));
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(existing));

        AppContent updated = service.update(CONTENT_ID, base().audienceScope(AudienceScope.ALL).build(), null, null);

        assertThat(updated.getAudienceScope()).isEqualTo(AudienceScope.ALL);
        assertThat(updated.getCourse()).isNull();
        assertThat(updated.getAcademicModule()).isNull();
        assertThat(updated.getStudyShift()).isNull();
    }

    @Test
    void narrowingTheAudienceKeepsOnlyTheTargetOfTheChosenScope() {
        when(academicModuleRepository.findById(3)).thenReturn(Optional.of(academicModule(3)));

        AppContent created = service.create(
                base().audienceScope(AudienceScope.MODULE).academicModuleId(3).courseId(7).build(), null, null);

        assertThat(created.getAcademicModule().getId()).isEqualTo(3);
        assertThat(created.getCourse()).as("courseId do request não vale para escopo MODULE").isNull();
    }

    @Test
    void anUnreadableYearIsARequestErrorAndNotASilentNull() {
        ContentRequest request = base().completionYear("dois mil e vinte").build();

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(request, null, null))
                .withMessageContaining("content.completion-year.invalid");
    }

    @Test
    void anEmptyYearIsSimplyAbsent() {
        AppContent created = service.create(base().completionYear("  ").build(), null, null);

        assertThat(created.getCompletionYear()).isNull();
    }

    /**
     * Publicado é o padrão: o formulário do painel não manda o campo quando o
     * bibliotecário não mexe no toggle, e comunicado que nasce escondido é
     * comunicado que ninguém lê.
     */
    @Test
    void contentIsPublishedUnlessSomebodySaysOtherwise() {
        assertThat(service.create(base().published(null).build(), null, null).getPublished()).isTrue();
        assertThat(service.create(base().published(false).build(), null, null).getPublished()).isFalse();
    }

    @Test
    void aFailedUploadIsARequestErrorAndNotAFiveHundred() {
        MockMultipartFile cover = new MockMultipartFile("coverFile", "capa.png", "image/png", new byte[] {1, 2, 3});
        when(storageProvider.upload(any(), eq(StorageBucket.COVERS))).thenThrow(new RuntimeException("bucket fora"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.create(base().build(), cover, null))
                .withMessageContaining("content.upload-failed");
    }

    @Test
    void anEmptyFilePartDoesNotOverwriteTheCurrentUrl() {
        MockMultipartFile empty = new MockMultipartFile("coverFile", "capa.png", "image/png", new byte[0]);

        assertThatCode(() -> service.create(base().build(), empty, empty)).doesNotThrowAnyException();

        verify(storageProvider, never()).upload(any(), any());
    }

    @Test
    void bothFilesLandInTheirOwnBucket() {
        MockMultipartFile cover = new MockMultipartFile("coverFile", "capa.png", "image/png", new byte[] {1});
        MockMultipartFile doc = new MockMultipartFile("docFile", "tcc.pdf", "application/pdf", new byte[] {2});
        when(storageProvider.upload(cover, StorageBucket.COVERS)).thenReturn("https://cdn/capa.png");
        when(storageProvider.upload(doc, StorageBucket.THESES)).thenReturn("https://cdn/tcc.pdf");

        AppContent created = service.create(base().build(), cover, doc);

        assertThat(created.getCoverUrl()).isEqualTo("https://cdn/capa.png");
        assertThat(created.getFileUrl()).isEqualTo("https://cdn/tcc.pdf");
    }

    // ---- remoção -------------------------------------------------------------

    /**
     * A remoção é lógica: o histórico de auditoria aponta para o id, e apagar a
     * linha transformaria a trilha em referência quebrada.
     */
    @Test
    void deletingOnlyStampsTheRemovalDate() {
        AppContent existing = published(AudienceScope.ALL);
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(existing));

        service.delete(CONTENT_ID);

        ArgumentCaptor<AppContent> captor = ArgumentCaptor.forClass(AppContent.class);
        verify(contentRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void removingTwiceIsNotFoundTheSecondTime() {
        AppContent removed = published(AudienceScope.ALL);
        removed.setDeletedAt(NOW.minusHours(1));
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(removed));

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.delete(CONTENT_ID));
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.update(CONTENT_ID, base().build(), null, null));
    }

    // ---- fixtures ------------------------------------------------------------

    private static ContentRequest.ContentRequestBuilder base() {
        return ContentRequest.builder().contentType(ContentType.ANNOUNCEMENT).title("Comunicado");
    }

    private static AppContent published(AudienceScope scope) {
        return AppContent.builder()
                .id(CONTENT_ID)
                .contentType(ContentType.ANNOUNCEMENT)
                .title("Comunicado")
                .published(true)
                .pinned(false)
                .displayOrder(0)
                .audienceScope(scope)
                .createdAt(NOW.minusDays(1))
                .updatedAt(NOW.minusDays(1))
                .build();
    }

    private void stored(AppContent content) {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
    }

    private static Course course(int id) {
        Course course = new Course();
        course.setId(id);
        course.setName("Curso " + id);
        return course;
    }

    private static AcademicModule academicModule(int id) {
        AcademicModule module = new AcademicModule();
        module.setId(id);
        module.setName("Modulo " + id);
        return module;
    }

    private static StudyShift studyShift(int id) {
        StudyShift shift = new StudyShift();
        shift.setId(id);
        shift.setName("Turno " + id);
        return shift;
    }

    private static void authenticateReader(Integer courseId, Integer moduleId, Integer shiftId) {
        Reader reader = new Reader();
        reader.setRegistrationNumber("2024001");
        if (courseId != null) {
            reader.setCourse(course(courseId));
        }
        if (moduleId != null) {
            reader.setAcademicModule(academicModule(moduleId));
        }
        if (shiftId != null) {
            reader.setStudyShift(studyShift(shiftId));
        }
        authenticate(AppUser.builder()
                .email("2024001@lumilivre.test")
                .passwordHash("hash")
                .role(Role.READER)
                .reader(reader)
                .build());
    }

    private static void authenticateStaff(Role role) {
        authenticate(AppUser.builder()
                .email("equipe@lumilivre.test")
                .passwordHash("hash")
                .role(role)
                .build());
    }

    private static void authenticate(AppUser appUser) {
        CustomUserDetails principal = new CustomUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
