package br.com.lumilivre.api.mapper.v2;

import br.com.lumilivre.api.dto.studyshift.StudyShiftRequest;
import br.com.lumilivre.api.dto.studyshift.StudyShiftResponse;
import br.com.lumilivre.api.dto.studyshift.StudyShiftSummaryResponse;
import br.com.lumilivre.api.dto.v1.turno.TurnoRequest;
import br.com.lumilivre.api.dto.v1.turno.TurnoResponse;
import br.com.lumilivre.api.dto.v1.turno.TurnoResumoResponse;
import org.springframework.stereotype.Component;

@Component
public class StudyShiftMapper {

    public StudyShiftSummaryResponse toSummary(TurnoResumoResponse v1) {
        return new StudyShiftSummaryResponse(v1.getId(), v1.getNome(), v1.getQuantidadeAlunos());
    }

    public StudyShiftResponse toResponse(TurnoResponse v1) {
        return new StudyShiftResponse(v1.getId(), v1.getNome());
    }

    public TurnoRequest toV1Request(StudyShiftRequest req) {
        return TurnoRequest.builder().nome(req.getName()).build();
    }
}
