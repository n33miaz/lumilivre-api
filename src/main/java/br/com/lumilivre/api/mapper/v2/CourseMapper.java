package br.com.lumilivre.api.mapper.v2;

import br.com.lumilivre.api.dto.course.CourseRequest;
import br.com.lumilivre.api.dto.course.CourseResponse;
import br.com.lumilivre.api.dto.course.CourseSummaryResponse;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.dto.v1.curso.CursoEstatisticaResponse;
import br.com.lumilivre.api.dto.v1.curso.CursoRequest;
import br.com.lumilivre.api.dto.v1.curso.CursoResponse;
import br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseSummaryResponse toSummary(CursoResumoResponse v1) {
        return new CourseSummaryResponse(v1.getId(), v1.getNome(), v1.getQuantidadeAlunos());
    }

    public CourseResponse toResponse(CursoResponse v1) {
        return new CourseResponse(v1.getId(), v1.getNome());
    }

    public CourseStatisticsResponse toStatistics(CursoEstatisticaResponse v1) {
        return new CourseStatisticsResponse(v1.getNomeCurso(), v1.getQuantidadeAlunos(), v1.getTotalEmprestimos());
    }

    public CursoRequest toV1Request(CourseRequest req) {
        return CursoRequest.builder().nome(req.getName()).build();
    }
}
