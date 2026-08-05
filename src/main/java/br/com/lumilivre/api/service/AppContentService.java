package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
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
import br.com.lumilivre.api.security.Auditable;
import br.com.lumilivre.api.security.CustomUserDetails;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppContentService {

    private final AppContentRepository contentRepository;
    private final StorageProvider storageProvider;
    private final CourseRepository courseRepository;
    private final AcademicModuleRepository academicModuleRepository;
    private final StudyShiftRepository studyShiftRepository;

    @Auditable(action = "CONTENT_CREATED", targetParam = "#result.id")
    @Transactional
    public AppContent create(ContentRequest request, MultipartFile coverFile, MultipartFile docFile) {
        validate(request);

        AppContent content = new AppContent();
        applyRequest(content, request);
        applyFiles(content, coverFile, docFile);

        return contentRepository.save(content);
    }

    @Auditable(action = "CONTENT_UPDATED", targetParam = "#id")
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
        String term = (q != null && !q.isBlank()) ? q.trim().toLowerCase(Locale.ROOT) : null;
        return contentRepository.findAll(adminSpec((root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (term != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + term + "%"),
                        cb.like(cb.lower(cb.coalesce(root.get("authors"), "")), "%" + term + "%")));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("contentType"), type));
            }
            return predicates;
        }));
    }

    @Transactional(readOnly = true)
    public List<AppContent> searchAdvanced(ContentType type, AudienceScope scope, Integer courseId, String year) {
        Integer parsedYear = parseYear(year);
        return contentRepository.findAll(adminSpec((root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null)       predicates.add(cb.equal(root.get("contentType"), type));
            if (scope != null)      predicates.add(cb.equal(root.get("audienceScope"), scope));
            if (courseId != null)   predicates.add(cb.equal(root.get("course").get("id"), courseId));
            if (parsedYear != null) predicates.add(cb.equal(root.get("completionYear"), parsedYear));
            return predicates;
        }));
    }

    private interface AdminPredicates {
        List<Predicate> build(jakarta.persistence.criteria.Root<AppContent> root,
                jakarta.persistence.criteria.CriteriaBuilder cb);
    }

    /**
     * Base das listagens do painel: exclui removidos, faz fetch das associações
     * exibidas (evita N+1 no mapper) e ordena como o mural (pin > ordem > data).
     */
    private static Specification<AppContent> adminSpec(AdminPredicates extra) {
        return (root, query, cb) -> {
            // O count query do executor não aceita fetch — só aplica na query de dados.
            if (query != null && AppContent.class.equals(query.getResultType())) {
                root.fetch("course", JoinType.LEFT);
                root.fetch("academicModule", JoinType.LEFT);
                root.fetch("studyShift", JoinType.LEFT);
                query.orderBy(
                        cb.desc(root.get("pinned")),
                        cb.asc(root.get("displayOrder")),
                        cb.desc(root.get("createdAt")));
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.addAll(extra.build(root, cb));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public AppContent getById(UUID id) {
        AppContent content = contentRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("content.not-found"));

        // Um READER só pode ler conteúdo que passaria no feed (publicado,
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

    @Auditable(action = "CONTENT_DELETED", targetParam = "#id")
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

    /** Mesma regra do feed, aplicada a um único conteúdo. */
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
