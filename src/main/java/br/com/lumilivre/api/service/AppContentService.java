package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.content.ContentRequest;
import br.com.lumilivre.api.enums.AudienceScope;
import br.com.lumilivre.api.enums.ContentType;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppContent;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppContentRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.security.CustomUserDetails;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppContentService {

    private final AppContentRepository contentRepository;
    private final StorageProvider storageProvider;
    private final CourseRepository courseRepository;
    private final AcademicModuleRepository academicModuleRepository;
    private final StudyShiftRepository studyShiftRepository;

    @Transactional
    public AppContent create(ContentRequest request, MultipartFile coverFile, MultipartFile docFile) {
        validate(request);

        AppContent content = new AppContent();
        applyRequest(content, request);
        applyFiles(content, coverFile, docFile);

        return contentRepository.save(content);
    }

    @Transactional
    public AppContent update(UUID id, ContentRequest request, MultipartFile coverFile, MultipartFile docFile) {
        validate(request);

        AppContent content = contentRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("content.not-found"));

        applyRequest(content, request);
        applyFiles(content, coverFile, docFile);

        return contentRepository.save(content);
    }

    @Transactional(readOnly = true)
    public List<AppContent> listForAdmin(String q, ContentType type) {
        String term = (q != null && !q.isBlank()) ? q.trim() : null;
        return contentRepository.findForAdmin(term, type);
    }

    @Transactional(readOnly = true)
    public List<AppContent> searchAdvanced(ContentType type, AudienceScope scope, Integer courseId, String year) {
        return contentRepository.searchAdvanced(type, scope, courseId, parseYear(year));
    }

    @Transactional(readOnly = true)
    public AppContent getById(UUID id) {
        AppContent content = contentRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("content.not-found"));

        // NEW-01: um READER só pode ler conteúdo que passaria no feed (publicado,
        // dentro da janela e com audiência compatível). ADMIN/LIBRARIAN veem tudo.
        // Caso contrário devolve 404 (não revela rascunhos/agendados/segmentados).
        if (isReaderOnly()) {
            Reader reader = currentReader();
            Integer rc = (reader != null && reader.getCourse() != null) ? reader.getCourse().getId() : null;
            Integer rm = (reader != null && reader.getAcademicModule() != null) ? reader.getAcademicModule().getId() : null;
            Integer rs = (reader != null && reader.getStudyShift() != null) ? reader.getStudyShift().getId() : null;
            if (!isVisibleToReader(content, rc, rm, rs)) {
                throw ResourceNotFoundException.ofKey("content.not-found");
            }
        }
        return content;
    }

    @Transactional
    public void delete(UUID id) {
        AppContent content = contentRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("content.not-found"));
        content.setDeletedAt(OffsetDateTime.now());
        contentRepository.save(content);
    }

    /**
     * Feed do leitor autenticado. Resolve curso/modulo/turno pelo principal do
     * JWT; leitores sem vinculo academico veem apenas conteudos de escopo ALL.
     */
    @Transactional(readOnly = true)
    public List<AppContent> feedForCurrentReader() {
        Reader reader = currentReader();
        Integer courseId = (reader != null && reader.getCourse() != null) ? reader.getCourse().getId() : null;
        Integer moduleId = (reader != null && reader.getAcademicModule() != null) ? reader.getAcademicModule().getId() : null;
        Integer shiftId = (reader != null && reader.getStudyShift() != null) ? reader.getStudyShift().getId() : null;
        return contentRepository.findFeed(courseId, moduleId, shiftId, OffsetDateTime.now());
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private void validate(ContentRequest request) {
        if (request.getContentType() == null) {
            throw BusinessRuleException.ofKey("content.type.required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw BusinessRuleException.ofKey("content.title.required");
        }
    }

    private void applyRequest(AppContent content, ContentRequest request) {
        content.setContentType(request.getContentType());
        content.setTitle(request.getTitle().trim());
        content.setBody(request.getBody());
        content.setAuthors(request.getAuthors());
        content.setAdvisors(request.getAdvisors());
        content.setCompletionYear(parseYear(request.getCompletionYear()));
        content.setCompletionSemester(request.getCompletionSemester());
        content.setExternalUrl(request.getExternalUrl());

        content.setPublished(request.getPublished() == null || request.getPublished());
        content.setPinned(Boolean.TRUE.equals(request.getPinned()));
        content.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        content.setPublishStartAt(request.getPublishStartAt());
        content.setPublishEndAt(request.getPublishEndAt());

        applyAudience(content, request);
    }

    private void applyAudience(AppContent content, ContentRequest request) {
        AudienceScope scope = request.getAudienceScope() != null ? request.getAudienceScope() : AudienceScope.ALL;
        content.setAudienceScope(scope);
        // Sempre limpa os alvos e recoloca apenas o correspondente ao escopo.
        content.setCourse(null);
        content.setAcademicModule(null);
        content.setStudyShift(null);

        switch (scope) {
            case COURSE -> {
                if (request.getCourseId() == null) {
                    throw BusinessRuleException.ofKey("content.audience.course-required");
                }
                content.setCourse(courseRepository.findById(request.getCourseId())
                        .orElseThrow(() -> ResourceNotFoundException.ofKey("course.not-found-with-id", request.getCourseId())));
            }
            case MODULE -> {
                if (request.getAcademicModuleId() == null) {
                    throw BusinessRuleException.ofKey("content.audience.module-required");
                }
                content.setAcademicModule(academicModuleRepository.findById(request.getAcademicModuleId())
                        .orElseThrow(() -> ResourceNotFoundException.ofKey("content.audience.module-not-found")));
            }
            case SHIFT -> {
                if (request.getStudyShiftId() == null) {
                    throw BusinessRuleException.ofKey("content.audience.shift-required");
                }
                content.setStudyShift(studyShiftRepository.findById(request.getStudyShiftId())
                        .orElseThrow(() -> ResourceNotFoundException.ofKey("content.audience.shift-not-found")));
            }
            case ALL -> {
                // sem alvo
            }
        }
    }

    private void applyFiles(AppContent content, MultipartFile coverFile, MultipartFile docFile) {
        try {
            if (coverFile != null && !coverFile.isEmpty()) {
                content.setCoverUrl(storageProvider.upload(coverFile, StorageBucket.COVERS));
            }
            if (docFile != null && !docFile.isEmpty()) {
                content.setFileUrl(storageProvider.upload(docFile, StorageBucket.THESES));
            }
        } catch (Exception e) {
            throw BusinessRuleException.ofKey("content.upload-failed");
        }
    }

    private Integer parseYear(String year) {
        if (year == null || year.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(year.trim());
        } catch (NumberFormatException e) {
            throw BusinessRuleException.ofKey("content.completion-year.invalid");
        }
    }

    private Reader currentReader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getAppUser().getReader();
        }
        return null;
    }

    /** True quando o principal autenticado é um LEITOR (não ADMIN/LIBRARIAN). */
    private boolean isReaderOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getPrincipal() instanceof CustomUserDetails details
                && details.getAppUser().getRole() == Role.READER;
    }

    /** Mesma regra do feed, aplicada a um único conteúdo (NEW-01). */
    private boolean isVisibleToReader(AppContent c, Integer readerCourseId,
            Integer readerModuleId, Integer readerShiftId) {
        if (!Boolean.TRUE.equals(c.getPublished())) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (c.getPublishStartAt() != null && c.getPublishStartAt().isAfter(now)) {
            return false;
        }
        if (c.getPublishEndAt() != null && c.getPublishEndAt().isBefore(now)) {
            return false;
        }
        return switch (c.getAudienceScope()) {
            case ALL -> true;
            case COURSE -> c.getCourse() != null && c.getCourse().getId().equals(readerCourseId);
            case MODULE -> c.getAcademicModule() != null && c.getAcademicModule().getId().equals(readerModuleId);
            case SHIFT -> c.getStudyShift() != null && c.getStudyShift().getId().equals(readerShiftId);
        };
    }
}
