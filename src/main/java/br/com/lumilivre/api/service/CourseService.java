package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.dto.course.CourseRequest;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.dto.course.CourseSummaryResponse;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.repository.CourseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    public Page<CourseSummaryResponse> buscarCursoParaListaAdmin(String texto, Pageable pageable) {
        return courseRepository.findSummariesByFilter(texto, pageable);
    }

    @Transactional
    public Course cadastrar(CourseRequest request) {
        Course curso = new Course();
        curso.setName(request.getName());
        return courseRepository.save(curso);
    }

    @Transactional
    public Course atualizar(Integer id, CourseRequest request) {
        Course curso = courseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("course.not-found-with-id", id));

        curso.setName(request.getName());
        return courseRepository.save(curso);
    }

    @Transactional
    public void excluir(Integer id) {
        if (!courseRepository.existsById(id)) {
            throw ResourceNotFoundException.ofKey("course.not-found-with-id", id);
        }
        courseRepository.deleteById(id);
    }

    public List<CourseStatisticsResponse> buscarEstatisticas() {
        return courseRepository.findStatistics();
    }

    public List<ChartItemResponse> buscarTotalEmprestimosPorCurso() {
        return courseRepository.findTotalEmprestimosPorCurso();
    }
}
