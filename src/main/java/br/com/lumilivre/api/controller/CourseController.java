package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.dto.course.CourseRequest;
import br.com.lumilivre.api.dto.course.CourseResponse;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.dto.course.CourseSummaryResponse;
import br.com.lumilivre.api.mapper.CourseMapper;
import br.com.lumilivre.api.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.COURSES)
public class CourseController {

    private final CourseService courseService;
    private final CourseMapper mapper;

    @GetMapping
    @Operation(operationId = "courses.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<Page<CourseSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<CourseSummaryResponse> page = courseService.buscarCursoParaListaAdmin(q, pageable);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @PostMapping
    @Operation(operationId = "courses.create")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CourseRequest request,
            Locale locale) {
        CourseResponse body = mapper.toResponse(courseService.cadastrar(request));
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}")
    @Operation(operationId = "courses.update")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody CourseRequest request,
            Locale locale) {
        CourseResponse body = mapper.toResponse(courseService.atualizar(id, request));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "courses.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        courseService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    @Operation(operationId = "courses.statistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<CourseStatisticsResponse>> statistics(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(courseService.buscarEstatisticas());
    }

    @GetMapping("/loan-statistics")
    @Operation(operationId = "courses.loanStatistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ChartItemResponse>> loanStatistics(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(courseService.buscarTotalEmprestimosPorCurso());
    }
}
