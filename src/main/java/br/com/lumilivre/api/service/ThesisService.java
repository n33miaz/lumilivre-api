package br.com.lumilivre.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.thesis.ThesisRequest;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Thesis;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.ThesisRepository;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ThesisService {

    private final ThesisRepository thesisRepository;
    private final StorageProvider storageProvider;
    private final CourseRepository courseRepository;

    @Transactional
    public Thesis createThesis(ThesisRequest request, MultipartFile pdfFile, MultipartFile coverFile) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw BusinessRuleException.ofKey("thesis.title.required");
        }
        if (request.getAuthors() == null || request.getAuthors().isBlank()) {
            throw BusinessRuleException.ofKey("thesis.authors.required");
        }
        if (request.getCourseId() == null) {
            throw BusinessRuleException.ofKey("thesis.course-id.required");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("course.not-found-with-id", request.getCourseId()));

        Thesis thesis = new Thesis();
        applyRequest(thesis, request, course);

        try {
            if (pdfFile != null && !pdfFile.isEmpty()) {
                thesis.setPdfUrl(storageProvider.upload(pdfFile, StorageBucket.THESES));
            }
            if (coverFile != null && !coverFile.isEmpty()) {
                thesis.setCoverUrl(storageProvider.upload(coverFile, StorageBucket.COVERS));
            }
        } catch (Exception e) {
            throw BusinessRuleException.ofKey("thesis.upload-failed");
        }

        return thesisRepository.save(thesis);
    }

    public List<Thesis> listTheses(String texto) {
        if (texto != null && !texto.isBlank()) {
            return thesisRepository.searchByText(texto);
        }
        return thesisRepository.findAllWithCourse();
    }

    public List<Thesis> searchTheses(Integer cursoId, String semestre, String ano) {
        Integer completionYear = parseCompletionYear(ano);
        return thesisRepository.searchAdvanced(cursoId, semestre, completionYear);
    }

    @Transactional
    public Thesis updateThesis(UUID id, ThesisRequest request, MultipartFile pdfFile, MultipartFile coverFile) {
        Thesis thesis = thesisRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("thesis.not-found"));

        Integer novoCursoId = request.getCourseId();
        Integer cursoAtualId = (thesis.getCourse() != null) ? thesis.getCourse().getId() : null;
        Course curso = thesis.getCourse();

        if (novoCursoId != null && !novoCursoId.equals(cursoAtualId)) {
            curso = courseRepository.findById(novoCursoId)
                    .orElseThrow(() -> ResourceNotFoundException.ofKey("course.not-found-with-id", novoCursoId));
        }

        applyRequest(thesis, request, curso);

        try {
            if (pdfFile != null && !pdfFile.isEmpty()) {
                thesis.setPdfUrl(storageProvider.upload(pdfFile, StorageBucket.THESES));
            }
            if (coverFile != null && !coverFile.isEmpty()) {
                thesis.setCoverUrl(storageProvider.upload(coverFile, StorageBucket.COVERS));
            }
        } catch (Exception e) {
            throw BusinessRuleException.ofKey("thesis.upload-failed");
        }

        return thesisRepository.save(thesis);
    }

    public Thesis getThesisById(UUID id) {
        return thesisRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("thesis.not-found"));
    }

    @Transactional
    public void deleteThesis(UUID id) {
        if (!thesisRepository.existsById(id)) {
            throw ResourceNotFoundException.ofKey("thesis.not-found");
        }
        thesisRepository.deleteById(id);
    }

    private void applyRequest(Thesis thesis, ThesisRequest request, Course course) {
        thesis.setTitle(request.getTitle());
        thesis.setAuthors(request.getAuthors());
        thesis.setAdvisors(request.getAdvisors());
        thesis.setCourse(course);
        thesis.setCompletionYear(parseCompletionYear(request.getCompletionYear()));
        thesis.setCompletionSemester(request.getCompletionSemester());
        thesis.setExternalUrl(request.getExternalUrl());
        thesis.setActive(request.getActive());
    }

    private Integer parseCompletionYear(String year) {
        if (year == null || year.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(year.trim());
        } catch (NumberFormatException e) {
            throw BusinessRuleException.ofKey("thesis.completion-year.invalid");
        }
    }
}
