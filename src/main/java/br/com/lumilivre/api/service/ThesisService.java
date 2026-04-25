package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.tcc.ThesisRequest;
import br.com.lumilivre.api.dto.tcc.ThesisResponse;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Thesis;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.ThesisRepository;
import br.com.lumilivre.api.service.infra.SupabaseStorageService;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ThesisService {

    private final ThesisRepository thesisRepository;
    private final SupabaseStorageService storageService;
    private final CourseRepository courseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ResponseEntity<ApiResponse<ThesisResponse>> createThesis(String dadosJson, MultipartFile arquivoPdf,
            MultipartFile arquivoFoto) {
        try {
            ThesisRequest dto = objectMapper.readValue(dadosJson, ThesisRequest.class);

            if (dto.getTitulo() == null || dto.getTitulo().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "O título do TCC é obrigatório.", null));
            }
            if (dto.getAlunos() == null || dto.getAlunos().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "O campo 'alunos' é obrigatório.", null));
            }
            if (dto.getCursoId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "O ID do curso é obrigatório.", null));
            }

            Course curso = courseRepository.findById(dto.getCursoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Curso com ID " + dto.getCursoId() + " não encontrado."));

            Thesis thesis = new Thesis();
            applyRequest(thesis, dto, curso);

            if (arquivoPdf != null && !arquivoPdf.isEmpty()) {
                String urlPdf = storageService.uploadFile(arquivoPdf, "tccs");
                thesis.setPdfUrl(urlPdf);
            }

            if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
                String urlFoto = storageService.uploadFile(arquivoFoto, "capas");
                thesis.setCoverUrl(urlFoto);
            }

            Thesis savedThesis = thesisRepository.save(thesis);
            ThesisResponse response = new ThesisResponse(savedThesis);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "TCC cadastrado com sucesso.", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Erro ao cadastrar TCC: " + e.getMessage(), null));
        }
    }

    public ResponseEntity<ApiResponse<List<ThesisResponse>>> listTheses(String texto) {
        List<Thesis> theses;

        if (texto != null && !texto.isBlank()) {
            theses = thesisRepository.searchByText(texto);
        } else {
            theses = thesisRepository.findAllWithCourse();
        }

        List<ThesisResponse> responses = theses.stream()
                .map(ThesisResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de TCCs obtida com sucesso.", responses));
    }

    public ResponseEntity<ApiResponse<List<ThesisResponse>>> searchTheses(
            Integer cursoId, String semestre, String ano) {

        Integer completionYear = parseCompletionYear(ano);
        List<Thesis> theses = thesisRepository.searchAdvanced(cursoId, semestre, completionYear);

        List<ThesisResponse> responses = theses.stream()
                .map(ThesisResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Lista filtrada com sucesso.", responses));
    }

    @Transactional
    public ResponseEntity<ApiResponse<ThesisResponse>> updateThesis(UUID id, String dadosJson, MultipartFile arquivoPdf,
            MultipartFile arquivoFoto) {
        try {
            Thesis thesis = thesisRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("TCC não encontrado."));

            ThesisRequest dto = objectMapper.readValue(dadosJson, ThesisRequest.class);

            Integer novoCursoId = dto.getCursoId();
            Integer cursoAtualId = (thesis.getCourse() != null) ? thesis.getCourse().getId() : null;
            Course curso = thesis.getCourse();

            if (novoCursoId != null && !novoCursoId.equals(cursoAtualId)) {
                curso = courseRepository.findById(novoCursoId)
                        .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
            }

            applyRequest(thesis, dto, curso);

            if (arquivoPdf != null && !arquivoPdf.isEmpty()) {
                String urlPdf = storageService.uploadFile(arquivoPdf, "tccs");
                thesis.setPdfUrl(urlPdf);
            }

            if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
                String urlFoto = storageService.uploadFile(arquivoFoto, "capas");
                thesis.setCoverUrl(urlFoto);
            }

            Thesis savedThesis = thesisRepository.save(thesis);
            return ResponseEntity.ok(new ApiResponse<>(true, "TCC atualizado com sucesso.", new ThesisResponse(savedThesis)));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Erro ao atualizar TCC: " + e.getMessage(), null));
        }
    }

    public ResponseEntity<ApiResponse<ThesisResponse>> getThesisById(UUID id) {
        return thesisRepository.findById(id)
                .map(thesis -> ResponseEntity.ok(new ApiResponse<>(true, "TCC encontrado.", new ThesisResponse(thesis))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "TCC não encontrado.", null)));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteThesis(UUID id) {
        if (!thesisRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "TCC não encontrado.", null));
        }
        thesisRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "TCC excluído com sucesso.", null));
    }

    private void applyRequest(Thesis thesis, ThesisRequest dto, Course course) {
        thesis.setTitle(dto.getTitulo());
        thesis.setAuthors(dto.getAlunos());
        thesis.setAdvisors(dto.getOrientadores());
        thesis.setCourse(course);
        thesis.setCompletionYear(parseCompletionYear(dto.getAnoConclusao()));
        thesis.setCompletionSemester(dto.getSemestreConclusao());
        thesis.setExternalUrl(dto.getLinkExterno());
        thesis.setActive(dto.getAtivo());
    }

    private Integer parseCompletionYear(String year) {
        if (year == null || year.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(year.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ano de conclusão inválido.");
        }
    }
}
