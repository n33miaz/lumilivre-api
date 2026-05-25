package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.lumilivre.api.dto.thesis.ThesisRequest;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Thesis;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.ThesisRepository;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ThesisServiceTest {

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MultipartFile pdfFile;

    @Mock
    private MultipartFile coverFile;

    @Captor
    private ArgumentCaptor<Thesis> thesisCaptor;

    @Test
    void createThesisUploadsFilesAndSavesMappedEntity() {
        Course course = new Course(7, "Computer Science", List.of());
        ThesisRequest request = request(7)
                .completionYear("2025")
                .completionSemester("2")
                .active(true)
                .build();
        when(courseRepository.findById(7)).thenReturn(Optional.of(course));
        when(pdfFile.isEmpty()).thenReturn(false);
        when(coverFile.isEmpty()).thenReturn(false);
        when(storageProvider.upload(pdfFile, StorageBucket.THESES)).thenReturn("https://cdn.test/thesis.pdf");
        when(storageProvider.upload(coverFile, StorageBucket.COVERS)).thenReturn("https://cdn.test/cover.jpg");
        when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Thesis result = service().createThesis(request, pdfFile, coverFile);

        assertThat(result.getTitle()).isEqualTo("Distributed Libraries");
        assertThat(result.getAuthors()).isEqualTo("Ada Lovelace; Grace Hopper");
        assertThat(result.getAdvisors()).isEqualTo("Donald Knuth");
        assertThat(result.getCourse()).isSameAs(course);
        assertThat(result.getCompletionYear()).isEqualTo(2025);
        assertThat(result.getCompletionSemester()).isEqualTo("2");
        assertThat(result.getExternalUrl()).isEqualTo("https://example.test/thesis");
        assertThat(result.getPdfUrl()).isEqualTo("https://cdn.test/thesis.pdf");
        assertThat(result.getCoverUrl()).isEqualTo("https://cdn.test/cover.jpg");
    }

    @Test
    void createThesisRejectsMissingRequiredFieldsBeforeRepositoryAccess() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createThesis(
                        request(1).title(" ").build(), null, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("thesis.title.required"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createThesis(
                        request(1).authors(null).build(), null, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("thesis.authors.required"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createThesis(
                        request(null).build(), null, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("thesis.course-id.required"));

        verify(courseRepository, never()).findById(any());
        verify(thesisRepository, never()).save(any());
    }

    @Test
    void createThesisRejectsMissingCourse() {
        when(courseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().createThesis(request(99).build(), null, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("course.not-found-with-id"));
        verify(thesisRepository, never()).save(any());
    }

    @Test
    void createThesisWrapsUploadFailure() {
        Course course = new Course(7, "Computer Science", List.of());
        when(courseRepository.findById(7)).thenReturn(Optional.of(course));
        when(pdfFile.isEmpty()).thenReturn(false);
        when(storageProvider.upload(pdfFile, StorageBucket.THESES)).thenThrow(new RuntimeException("storage down"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createThesis(request(7).build(), pdfFile, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("thesis.upload-failed"));
        verify(thesisRepository, never()).save(any());
    }

    @Test
    void listThesesDelegatesToTextSearchWhenQueryIsPresent() {
        Thesis thesis = Thesis.builder().title("Searchable").build();
        when(thesisRepository.searchByText("search")).thenReturn(List.of(thesis));

        assertThat(service().listTheses("search")).containsExactly(thesis);
        verify(thesisRepository, never()).findAllWithCourse();
    }

    @Test
    void listThesesFetchesAllWithCourseWhenQueryIsBlank() {
        Thesis thesis = Thesis.builder().title("All").build();
        when(thesisRepository.findAllWithCourse()).thenReturn(List.of(thesis));

        assertThat(service().listTheses(" ")).containsExactly(thesis);
        verify(thesisRepository, never()).searchByText(any());
    }

    @Test
    void searchThesesParsesCompletionYear() {
        service().searchTheses(7, "1", " 2024 ");

        verify(thesisRepository).searchAdvanced(7, "1", 2024);
    }

    @Test
    void searchThesesRejectsInvalidCompletionYear() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().searchTheses(7, "1", "twenty"))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("thesis.completion-year.invalid"));
        verify(thesisRepository, never()).searchAdvanced(any(), any(), any());
    }

    @Test
    void updateThesisKeepsCurrentCourseWhenRequestedCourseIsSame() {
        UUID id = UUID.randomUUID();
        Course course = new Course(7, "Computer Science", List.of());
        Thesis thesis = Thesis.builder()
                .id(id)
                .title("Old")
                .authors("Old Author")
                .course(course)
                .pdfUrl("https://cdn.test/old.pdf")
                .coverUrl("https://cdn.test/old.jpg")
                .build();
        ThesisRequest request = request(7)
                .title("New")
                .completionYear(null)
                .build();
        when(thesisRepository.findById(id)).thenReturn(Optional.of(thesis));
        when(coverFile.isEmpty()).thenReturn(false);
        when(storageProvider.upload(coverFile, StorageBucket.COVERS)).thenReturn("https://cdn.test/new.jpg");
        when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Thesis result = service().updateThesis(id, request, null, coverFile);

        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getCourse()).isSameAs(course);
        assertThat(result.getCompletionYear()).isNull();
        assertThat(result.getPdfUrl()).isEqualTo("https://cdn.test/old.pdf");
        assertThat(result.getCoverUrl()).isEqualTo("https://cdn.test/new.jpg");
        verify(courseRepository, never()).findById(any());
    }

    @Test
    void updateThesisChangesCourseWhenRequestedCourseDiffers() {
        UUID id = UUID.randomUUID();
        Course oldCourse = new Course(7, "Computer Science", List.of());
        Course newCourse = new Course(8, "Information Systems", List.of());
        Thesis thesis = Thesis.builder().id(id).course(oldCourse).build();
        when(thesisRepository.findById(id)).thenReturn(Optional.of(thesis));
        when(courseRepository.findById(8)).thenReturn(Optional.of(newCourse));
        when(thesisRepository.save(any(Thesis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Thesis result = service().updateThesis(id, request(8).build(), null, null);

        assertThat(result.getCourse()).isSameAs(newCourse);
    }

    @Test
    void updateThesisRejectsMissingThesis() {
        UUID id = UUID.randomUUID();
        when(thesisRepository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().updateThesis(id, request(1).build(), null, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("thesis.not-found"));
    }

    @Test
    void getThesisByIdReturnsExistingThesis() {
        UUID id = UUID.randomUUID();
        Thesis thesis = Thesis.builder().id(id).title("Existing").build();
        when(thesisRepository.findById(id)).thenReturn(Optional.of(thesis));

        assertThat(service().getThesisById(id)).isSameAs(thesis);
    }

    @Test
    void deleteThesisDeletesExistingId() {
        UUID id = UUID.randomUUID();
        when(thesisRepository.existsById(id)).thenReturn(true);

        service().deleteThesis(id);

        verify(thesisRepository).deleteById(id);
    }

    @Test
    void deleteThesisRejectsMissingId() {
        UUID id = UUID.randomUUID();
        when(thesisRepository.existsById(id)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().deleteThesis(id))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("thesis.not-found"));
        verify(thesisRepository, never()).deleteById(any());
    }

    private ThesisService service() {
        return new ThesisService(thesisRepository, storageProvider, courseRepository);
    }

    private static ThesisRequest.ThesisRequestBuilder request(Integer courseId) {
        return ThesisRequest.builder()
                .title("Distributed Libraries")
                .authors("Ada Lovelace; Grace Hopper")
                .advisors("Donald Knuth")
                .courseId(courseId)
                .completionYear("2025")
                .completionSemester("1")
                .externalUrl("https://example.test/thesis")
                .active(true);
    }
}
