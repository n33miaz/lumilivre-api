package br.com.lumilivre.api.controller.v2;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.dto.course.CourseRequest;
import br.com.lumilivre.api.dto.course.CourseResponse;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.dto.course.CourseSummaryResponse;
import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.mapper.v2.CourseMapper;
import br.com.lumilivre.api.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<Page<CourseSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<CourseSummaryResponse> page = courseService
                .buscarCursoParaListaAdmin(q, pageable)
                .map(mapper::toSummary);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CourseRequest request,
            Locale locale) {
        var v1 = courseService.cadastrar(mapper.toV1Request(request));
        return ResponseEntity.status(v1.getStatusCode())
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(v1.getBody()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CourseRequest request,
            Locale locale) {
        var v1 = courseService.atualizar(id, mapper.toV1Request(request));
        return ResponseEntity.status(v1.getStatusCode())
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(v1.getBody()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        courseService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<CourseStatisticsResponse>> statistics(Locale locale) {
        List<CourseStatisticsResponse> body = courseService.buscarEstatisticas()
                .stream()
                .map(mapper::toStatistics)
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/loan-statistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ChartItemResponse>> loanStatistics(Locale locale) {
        List<ChartItemResponse> body = courseService.buscarTotalEmprestimosPorCurso()
                .stream()
                .map(item -> new ChartItemResponse(item.nome(), item.total()))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }
}
