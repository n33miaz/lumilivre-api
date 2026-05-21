package br.com.lumilivre.api.controller;

import java.util.Locale;
import java.util.List;

import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.dto.studyshift.StudyShiftRequest;
import br.com.lumilivre.api.dto.studyshift.StudyShiftResponse;
import br.com.lumilivre.api.dto.studyshift.StudyShiftSummaryResponse;
import br.com.lumilivre.api.mapper.StudyShiftMapper;
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
@RequestMapping("/api/study-shifts")
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
        Page<StudyShiftSummaryResponse> page = studyShiftService.buscarPorTexto(q, pageable);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<StudyShiftResponse> create(
            @Valid @RequestBody StudyShiftRequest request,
            Locale locale) {
        StudyShiftResponse body = mapper.toResponse(studyShiftService.cadastrar(request));
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<StudyShiftResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody StudyShiftRequest request,
            Locale locale) {
        StudyShiftResponse body = mapper.toResponse(studyShiftService.atualizar(id, request));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
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
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(studyShiftService.buscarTotalEmprestimosPorTurno());
    }
}
