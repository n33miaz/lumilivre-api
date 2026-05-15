package br.com.lumilivre.api.controller.v2;

import java.util.Locale;
import java.util.List;

import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.dto.studyshift.StudyShiftRequest;
import br.com.lumilivre.api.dto.studyshift.StudyShiftResponse;
import br.com.lumilivre.api.dto.studyshift.StudyShiftSummaryResponse;
import br.com.lumilivre.api.mapper.v2.StudyShiftMapper;
import br.com.lumilivre.api.service.StudyShiftService;
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
@RequestMapping("/api/v2/study-shifts")
@RequiredArgsConstructor
public class StudyShiftController {

    private final StudyShiftService studyShiftService;
    private final StudyShiftMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<Page<StudyShiftSummaryResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<StudyShiftSummaryResponse> page = studyShiftService
                .buscarPorTexto(q, pageable)
                .map(mapper::toSummary);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<StudyShiftResponse> create(
            @Valid @RequestBody StudyShiftRequest request,
            Locale locale) {
        var v1 = studyShiftService.cadastrar(mapper.toV1Request(request));
        return ResponseEntity.status(v1.getStatusCode())
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(v1.getBody()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<StudyShiftResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody StudyShiftRequest request,
            Locale locale) {
        var v1 = studyShiftService.atualizar(id, mapper.toV1Request(request));
        return ResponseEntity.status(v1.getStatusCode())
                .header("Content-Language", locale.toLanguageTag())
                .body(mapper.toResponse(v1.getBody()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        studyShiftService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/loan-statistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ChartItemResponse>> loanStatistics(Locale locale) {
        List<ChartItemResponse> body = studyShiftService.buscarTotalEmprestimosPorTurno()
                .stream()
                .map(item -> new ChartItemResponse(item.nome(), item.total()))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }
}
