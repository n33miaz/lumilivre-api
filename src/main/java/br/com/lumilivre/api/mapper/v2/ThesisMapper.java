package br.com.lumilivre.api.mapper.v2;

import br.com.lumilivre.api.dto.thesis.ThesisRequest;
import br.com.lumilivre.api.dto.thesis.ThesisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThesisMapper {

    private final ObjectMapper objectMapper;

    public ThesisResponse fromV1(br.com.lumilivre.api.dto.v1.tcc.ThesisResponse v1) {
        return ThesisResponse.builder()
                .id(v1.getId())
                .title(v1.getTitulo())
                .authors(v1.getAlunos())
                .advisors(v1.getOrientadores())
                .courseName(v1.getCurso())
                .completionYear(v1.getAnoConclusao())
                .completionSemester(v1.getSemestreConclusao())
                .pdfUrl(v1.getArquivoPdf())
                .coverUrl(v1.getFoto())
                .externalUrl(v1.getLinkExterno())
                .active(v1.getAtivo())
                .build();
    }

    public br.com.lumilivre.api.dto.v1.tcc.ThesisRequest toV1Request(ThesisRequest req) {
        return br.com.lumilivre.api.dto.v1.tcc.ThesisRequest.builder()
                .titulo(req.getTitle())
                .alunos(req.getAuthors())
                .orientadores(req.getAdvisors())
                .cursoId(req.getCourseId())
                .anoConclusao(req.getCompletionYear())
                .semestreConclusao(req.getCompletionSemester())
                .linkExterno(req.getExternalUrl())
                .ativo(req.getActive())
                .build();
    }

    public String toV1Json(ThesisRequest req) {
        try {
            return objectMapper.writeValueAsString(toV1Request(req));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize thesis request", e);
        }
    }
}
